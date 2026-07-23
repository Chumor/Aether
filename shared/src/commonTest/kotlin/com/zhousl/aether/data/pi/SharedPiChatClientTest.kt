package com.zhousl.aether.data.pi

import com.zhousl.aether.data.LlmProviderConfig
import com.zhousl.aether.runtime.PiBridgeTransport
import com.zhousl.aether.runtime.RuntimeProcess
import com.zhousl.aether.runtime.RuntimeProcessExit
import com.zhousl.aether.runtime.RuntimeProcessSignal
import com.zhousl.aether.runtime.SharedPiBridgeClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SharedPiChatClientTest {
    @Test
    fun sendsMultimodalContentAndParsesUsage() = runTest {
        val process = ChatProtocolProcess()
        val bridge = SharedPiBridgeClient(
            transport = SingleProcessTransport(process),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        val client = SharedPiChatClient(bridge)

        val result = client.runTurn(
            config = testProvider(),
            messages = listOf(
                SharedPiChatMessage(
                    role = "user",
                    text = "describe",
                    images = listOf(SharedPiImage("image/png", "AQID")),
                )
            ),
            sessionId = "session-1",
        )

        val request = process.requests.single()
        val payload = request["payload"]!!.jsonObject
        val content = payload["messages"]!!.jsonArray.single().jsonObject["content"]!!.jsonArray
        assertEquals("text", content[0].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("image/png", content[1].jsonObject["mime_type"]!!.jsonPrimitive.content)
        assertEquals("AQID", content[1].jsonObject["data"]!!.jsonPrimitive.content)
        assertEquals(7, result.usage.inputTokens)
        assertEquals(11, result.usage.outputTokens)
        assertEquals(18, result.usage.totalTokens)
        bridge.close()
    }

    @Test
    fun steerTargetsRunningSession() = runTest {
        val process = ChatProtocolProcess()
        val bridge = SharedPiBridgeClient(
            transport = SingleProcessTransport(process),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        val client = SharedPiChatClient(bridge)

        assertTrue(client.steer("session-2", SharedPiChatMessage("user", "change direction")))

        val payload = process.requests.single()["payload"]!!.jsonObject
        assertEquals("session-2", payload["session_id"]!!.jsonPrimitive.content)
        assertEquals("change direction", payload["message"]!!.jsonObject["text"]!!.jsonPrimitive.content)
        bridge.close()
    }
}

private class SingleProcessTransport(private val process: RuntimeProcess) : PiBridgeTransport {
    override suspend fun start(): RuntimeProcess = process
    override suspend fun stop() = Unit
}

private class ChatProtocolProcess : RuntimeProcess {
    private val output = Channel<ByteArray>(Channel.UNLIMITED)
    val requests = mutableListOf<JsonObject>()
    override val pid: Int = 21
    override val stdout: Flow<ByteArray> = output.receiveAsFlow()
    override val stderr: Flow<ByteArray> = Channel<ByteArray>().receiveAsFlow()

    override suspend fun writeStdin(bytes: ByteArray) {
        val request = Json.parseToJsonElement(bytes.decodeToString().trim()).jsonObject
        requests += request
        val id = request["id"]!!.jsonPrimitive.content
        val type = request["type"]!!.jsonPrimitive.content
        val payload = if (type == "steer") {
            buildJsonObject { put("accepted", true) }
        } else {
            buildJsonObject {
                put("assistant_text", "done")
                put("usage", buildJsonObject {
                    put("input_tokens", 7)
                    put("output_tokens", 11)
                    put("total_tokens", 18)
                })
            }
        }
        output.send((buildJsonObject {
            put("type", "response")
            put("id", id)
            put("ok", true)
            put("payload", payload)
        }.toString() + "\n").encodeToByteArray())
    }

    override suspend fun closeStdin() = Unit
    override suspend fun awaitExit(): RuntimeProcessExit = CompletableDeferred<RuntimeProcessExit>().await()
    override suspend fun signal(signal: RuntimeProcessSignal) = Unit
}

private fun testProvider() = LlmProviderConfig(
    providerId = "test",
    name = "Test",
    piProviderId = "openai-compatible",
    apiKey = "key",
    baseUrl = "https://example.com/v1",
    modelId = "test-model",
)
