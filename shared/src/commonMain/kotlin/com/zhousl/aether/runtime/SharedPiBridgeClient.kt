package com.zhousl.aether.runtime

import com.zhousl.aether.data.platformRandomUuid
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class PiBridgeRequestException(
    message: String,
    val code: String = "pi_bridge_error",
) : IllegalStateException(message)

class SharedPiBridgeClient(
    private val transport: PiBridgeTransport,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private data class PendingRequest(
        val response: CompletableDeferred<JsonObject>,
        val events: Channel<Pair<String, JsonObject>>,
        val eventJob: Job,
    )

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val stateMutex = Mutex()
    private val writeMutex = Mutex()
    private val pending = mutableMapOf<String, PendingRequest>()
    private var process: RuntimeProcess? = null
    private var readerJob: Job? = null

    suspend fun ping(): JsonObject = request("ping", timeoutMillis = 15_000)

    suspend fun listProviders(): JsonObject =
        request("list_providers", timeoutMillis = 15_000)

    suspend fun loginProvider(
        providerConfigId: String,
        providerId: String,
        authMethod: String,
        oauthFlow: String,
        onEvent: suspend (String, JsonObject) -> Unit,
    ): JsonObject = request(
        type = "login_provider",
        payload = buildJsonObject {
            put("provider_config_id", providerConfigId)
            put("provider_id", providerId)
            put("auth_method", authMethod)
            put("oauth_flow", oauthFlow)
        },
        timeoutMillis = 15 * 60_000L,
        onEvent = onEvent,
    )

    suspend fun submitAuthPrompt(promptId: String, value: String, cancelled: Boolean): JsonObject =
        request(
            type = "auth_prompt_result",
            payload = buildJsonObject {
                put("prompt_id", promptId)
                put("value", value)
                put("cancelled", cancelled)
            },
            timeoutMillis = 15_000,
            abortOnCancellation = false,
        )

    suspend fun clearProviderCredential(providerConfigId: String): JsonObject =
        request(
            type = "clear_provider_credential",
            payload = buildJsonObject { put("provider_config_id", providerConfigId) },
            timeoutMillis = 15_000,
        )

    suspend fun listExtensionPackages(): JsonObject =
        request("list_extension_packages", timeoutMillis = 30_000, abortOnCancellation = false)

    suspend fun installExtensionPackage(source: String): JsonObject =
        request(
            type = "install_extension_package",
            payload = buildJsonObject { put("source", source) },
            timeoutMillis = 10 * 60_000L,
            abortOnCancellation = false,
        )

    suspend fun updateExtensionPackage(source: String): JsonObject =
        request(
            type = "update_extension_package",
            payload = buildJsonObject { put("source", source) },
            timeoutMillis = 10 * 60_000L,
            abortOnCancellation = false,
        )

    suspend fun removeExtensionPackage(source: String): JsonObject =
        request(
            type = "remove_extension_package",
            payload = buildJsonObject { put("source", source) },
            timeoutMillis = 10 * 60_000L,
            abortOnCancellation = false,
        )

    suspend fun getAetherExtensions(
        context: JsonObject = JsonObject(emptyMap()),
        onEvent: suspend (String, JsonObject) -> Unit = { _, _ -> },
    ): JsonObject = request(
        type = "get_aether_extensions",
        payload = aetherExtensionPayload(context),
        timeoutMillis = 10 * 60_000L,
        onEvent = onEvent,
        abortOnCancellation = false,
    )

    suspend fun reloadAetherExtensions(
        context: JsonObject = JsonObject(emptyMap()),
        onEvent: suspend (String, JsonObject) -> Unit = { _, _ -> },
    ): JsonObject = request(
        type = "reload_aether_extensions",
        payload = aetherExtensionPayload(context),
        timeoutMillis = 10 * 60_000L,
        onEvent = onEvent,
        abortOnCancellation = false,
    )

    suspend fun invokeAetherExtensionAction(
        extensionId: String,
        action: String,
        args: JsonObject = JsonObject(emptyMap()),
        context: JsonObject = JsonObject(emptyMap()),
        onEvent: suspend (String, JsonObject) -> Unit = { _, _ -> },
    ): JsonObject = request(
        type = "invoke_aether_extension_action",
        payload = buildJsonObject {
            put("extension_id", extensionId)
            put("action", action)
            put("args", args)
            put("context", context)
        },
        timeoutMillis = 10 * 60_000L,
        onEvent = onEvent,
        abortOnCancellation = false,
    )

    suspend fun dispatchAetherExtensionEvent(
        event: String,
        data: JsonObject = JsonObject(emptyMap()),
        context: JsonObject = JsonObject(emptyMap()),
        onEvent: suspend (String, JsonObject) -> Unit = { _, _ -> },
    ): JsonObject = request(
        type = "dispatch_aether_extension_event",
        payload = buildJsonObject {
            put("event", event)
            put("data", data)
            put("context", context)
        },
        timeoutMillis = 10 * 60_000L,
        onEvent = onEvent,
        abortOnCancellation = false,
    )

    suspend fun sendAetherHostResult(
        callId: String,
        result: JsonObject = JsonObject(emptyMap()),
        error: String = "",
    ): JsonObject = request(
        type = "aether_host_result",
        payload = buildJsonObject {
            put("call_id", callId)
            put("ok", error.isBlank())
            put("result", result)
            put("error", error)
        },
        timeoutMillis = 15_000L,
        abortOnCancellation = false,
    )

    suspend fun request(
        type: String,
        payload: JsonObject = JsonObject(emptyMap()),
        timeoutMillis: Long = 10 * 60_000L,
        onEvent: suspend (String, JsonObject) -> Unit = { _, _ -> },
        abortOnCancellation: Boolean = type in setOf("run_turn", "complete_once", "follow_up", "login_provider"),
    ): JsonObject {
        val activeProcess = ensureStarted()
        val requestId = "$type-${platformRandomUuid()}"
        val deferred = CompletableDeferred<JsonObject>()
        val events = Channel<Pair<String, JsonObject>>(Channel.UNLIMITED)
        val eventJob = scope.launch {
            for ((event, eventPayload) in events) onEvent(event, eventPayload)
        }
        stateMutex.withLock {
            pending[requestId] = PendingRequest(deferred, events, eventJob)
        }
        try {
            val frame = buildJsonObject {
                put("id", requestId)
                put("type", type)
                put("payload", payload)
            }
            writeMutex.withLock {
                activeProcess.writeStdin(BridgeFrameCodec().encode(frame))
            }
            return withTimeout(timeoutMillis) { deferred.await() }
        } catch (throwable: Throwable) {
            if (abortOnCancellation && throwable is CancellationException) {
                runCatching {
                    request(
                        type = "abort",
                        payload = buildJsonObject { put("request_id", requestId) },
                        timeoutMillis = 15_000,
                        abortOnCancellation = false,
                    )
                }
            }
            throw throwable
        } finally {
            stateMutex.withLock { pending.remove(requestId) }
            events.close()
            eventJob.cancel()
        }
    }

    suspend fun close() {
        stateMutex.withLock {
            pending.values.forEach { request ->
                request.events.close()
                request.eventJob.cancel()
                request.response.completeExceptionally(PiBridgeRequestException("Pi Bridge closed."))
            }
            pending.clear()
            readerJob?.cancel()
            readerJob = null
            process = null
        }
        transport.stop()
        scope.cancel()
    }

    private suspend fun ensureStarted(): RuntimeProcess {
        stateMutex.withLock { process?.let { return it } }
        val started = transport.start()
        stateMutex.withLock {
            process?.let { return it }
            process = started
            readerJob = scope.launch { readFrames(started) }
        }
        return started
    }

    private suspend fun readFrames(activeProcess: RuntimeProcess) {
        val codec = BridgeFrameCodec()
        try {
            activeProcess.stdout.collect { chunk ->
                codec.append(chunk).forEach { dispatchFrame(it) }
            }
            val exit = activeProcess.awaitExit()
            failAll("Pi Bridge exited with code ${exit.exitCode}.")
        } catch (throwable: Throwable) {
            if (throwable !is CancellationException) {
                failAll(throwable.message ?: "Pi Bridge output failed.")
            }
        } finally {
            stateMutex.withLock {
                if (process === activeProcess) process = null
            }
        }
    }

    private suspend fun dispatchFrame(frame: JsonObject) {
        val id = frame.string("id")
        val request = stateMutex.withLock { pending[id] } ?: return
        when (frame.string("type")) {
            "event" -> request.events.send(
                frame.string("event") to
                    (frame["payload"] as? JsonObject ?: JsonObject(emptyMap()))
            )
            "error" -> {
                val error = frame["error"] as? JsonObject
                request.events.close()
                scope.launch {
                    request.eventJob.join()
                    request.response.completeExceptionally(
                        PiBridgeRequestException(
                            message = error?.string("message").orEmpty().ifBlank { "Pi Bridge request failed." },
                            code = error?.string("code").orEmpty().ifBlank { "pi_bridge_error" },
                        )
                    )
                }
            }
            "response" -> {
                val ok = frame["ok"]?.jsonPrimitive?.booleanOrNull ?: true
                request.events.close()
                scope.launch {
                    request.eventJob.join()
                    if (ok) {
                        request.response.complete(frame["payload"] as? JsonObject ?: JsonObject(emptyMap()))
                    } else {
                        request.response.completeExceptionally(PiBridgeRequestException("Pi Bridge request failed."))
                    }
                }
            }
        }
    }

    private suspend fun failAll(message: String) {
        stateMutex.withLock {
            pending.values.forEach { request ->
                request.events.close()
                request.eventJob.cancel()
                request.response.completeExceptionally(PiBridgeRequestException(message))
            }
            pending.clear()
        }
    }
}

private fun aetherExtensionPayload(context: JsonObject): JsonObject = buildJsonObject {
    put("disabled_extension_paths", buildJsonArray {})
    put("disabled_package_sources", buildJsonArray {})
    put("context", context)
}

private fun JsonObject.string(name: String): String =
    get(name)?.jsonPrimitive?.contentOrNull.orEmpty()
