package com.zhousl.aether.runtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedPiBridgeClientTest {
    @Test
    fun dispatchesEventsBeforeCompletingResponse() = runTest {
        val process = ProtocolFakeProcess()
        val client = SharedPiBridgeClient(
            transport = FakeBridgeTransport(process),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        val events = mutableListOf<String>()

        val response = client.request("login_provider", onEvent = { event, _ -> events += event })

        assertEquals(listOf("auth_progress"), events)
        assertEquals("ready", response["status"]?.jsonPrimitive?.content)
        client.close()
    }

    @Test
    fun allowsEventHandlerToSendNestedBridgeRequest() = runTest {
        val process = NestedRequestFakeProcess()
        val client = SharedPiBridgeClient(
            transport = FakeBridgeTransport(process),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val response = client.request(
            type = "run_turn",
            onEvent = { event, _ ->
                assertEquals("host_tool_request", event)
                client.request("host_tool_result", abortOnCancellation = false)
            },
        )

        assertEquals("complete", response["status"]?.jsonPrimitive?.content)
        client.close()
    }
}

private class FakeBridgeTransport(
    private val process: RuntimeProcess,
) : PiBridgeTransport {
    override suspend fun start(): RuntimeProcess = process
    override suspend fun stop() = Unit
}

private class ProtocolFakeProcess : RuntimeProcess {
    private val output = Channel<ByteArray>(Channel.UNLIMITED)
    override val pid: Int = 7
    override val stdout: Flow<ByteArray> = output.receiveAsFlow()
    override val stderr: Flow<ByteArray> = Channel<ByteArray>().receiveAsFlow()

    override suspend fun writeStdin(bytes: ByteArray) {
        val request = Json.parseToJsonElement(bytes.decodeToString().trim()).jsonObject
        val id = request["id"]!!.jsonPrimitive.content
        output.send(frame("event", id, buildJsonObject { put("message", "working") }, "auth_progress"))
        output.send(frame("response", id, buildJsonObject { put("status", "ready") }))
    }

    override suspend fun closeStdin() = Unit
    override suspend fun awaitExit(): RuntimeProcessExit = CompletableDeferred<RuntimeProcessExit>().await()
    override suspend fun signal(signal: RuntimeProcessSignal) = Unit

    private fun frame(type: String, id: String, payload: JsonObject, event: String = ""): ByteArray =
        (buildJsonObject {
            put("type", type)
            put("id", id)
            put("ok", true)
            if (event.isNotBlank()) put("event", event)
            put("payload", payload)
        }.toString() + "\n").encodeToByteArray()
}

private class NestedRequestFakeProcess : RuntimeProcess {
    private val output = Channel<ByteArray>(Channel.UNLIMITED)
    private var outerRequestId = ""
    override val pid: Int = 8
    override val stdout: Flow<ByteArray> = output.receiveAsFlow()
    override val stderr: Flow<ByteArray> = Channel<ByteArray>().receiveAsFlow()

    override suspend fun writeStdin(bytes: ByteArray) {
        val request = Json.parseToJsonElement(bytes.decodeToString().trim()).jsonObject
        val id = request["id"]!!.jsonPrimitive.content
        when (request["type"]!!.jsonPrimitive.content) {
            "run_turn" -> {
                outerRequestId = id
                output.send(frame("event", id, JsonObject(emptyMap()), "host_tool_request"))
            }
            "host_tool_result" -> {
                output.send(frame("response", id, buildJsonObject { put("accepted", true) }))
                output.send(frame("response", outerRequestId, buildJsonObject { put("status", "complete") }))
            }
        }
    }

    override suspend fun closeStdin() = Unit
    override suspend fun awaitExit(): RuntimeProcessExit = CompletableDeferred<RuntimeProcessExit>().await()
    override suspend fun signal(signal: RuntimeProcessSignal) = Unit

    private fun frame(type: String, id: String, payload: JsonObject, event: String = ""): ByteArray =
        (buildJsonObject {
            put("type", type)
            put("id", id)
            put("ok", true)
            if (event.isNotBlank()) put("event", event)
            put("payload", payload)
        }.toString() + "\n").encodeToByteArray()
}
