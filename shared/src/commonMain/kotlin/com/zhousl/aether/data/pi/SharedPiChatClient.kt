package com.zhousl.aether.data.pi

import com.zhousl.aether.data.LlmProviderConfig
import com.zhousl.aether.data.PiProviderCatalog
import com.zhousl.aether.data.ProviderAuthMethod
import com.zhousl.aether.data.normalizeLlmUserAgent
import com.zhousl.aether.data.platformDefaultSystemPrompt
import com.zhousl.aether.data.platformRandomUuid
import com.zhousl.aether.runtime.SharedPiBridgeClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class SharedPiChatMessage(
    val role: String,
    val text: String,
    val images: List<SharedPiImage> = emptyList(),
)

data class SharedPiImage(
    val mimeType: String,
    val data: String,
)

data class SharedPiTurnResult(
    val assistantText: String,
    val reasoningText: String = "",
    val provider: String = "",
    val model: String = "",
    val errorMessage: String = "",
    val usage: SharedPiUsage = SharedPiUsage(),
)

data class SharedPiUsage(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val totalTokens: Long = 0,
    val reasoningTokens: Long = 0,
    val cachedInputTokens: Long = 0,
)

class SharedPiChatClient(
    private val bridge: SharedPiBridgeClient,
    private val hostToolExecutor: SharedHostToolExecutor? = null,
) {
    suspend fun steer(
        sessionId: String,
        message: SharedPiChatMessage,
    ): Boolean {
        val response = bridge.request(
            type = "steer",
            payload = buildJsonObject {
                put("session_id", sessionId)
                put("message", message.toPiMessage())
            },
            abortOnCancellation = false,
        )
        return response["accepted"]?.jsonPrimitive?.booleanOrNull == true
    }

    suspend fun runTurn(
        config: LlmProviderConfig,
        messages: List<SharedPiChatMessage>,
        sessionId: String,
        workspaceDirectory: String = "/workspace",
        systemPrompt: String = platformDefaultSystemPrompt(),
        reasoning: String = "off",
        onAssistantTextDelta: suspend (String) -> Unit = {},
        onAssistantReasoningDelta: suspend (String) -> Unit = {},
        onHostToolStarted: suspend (SharedPiHostToolCall) -> Unit = {},
        onHostToolFinished: suspend (SharedPiHostToolCall, SharedHostToolResult) -> Unit = { _, _ -> },
    ): SharedPiTurnResult {
        val payload = buildJsonObject {
            put("model_config", config.toSharedPiModelConfig())
            put("session_id", sessionId.ifBlank { "aether-session-${platformRandomUuid()}" })
            put("system_prompt", systemPrompt.ifBlank { platformDefaultSystemPrompt() })
            put("messages", messages.toPiMessages())
            put("workspace_directory", workspaceDirectory)
            put("reasoning", reasoning)
            put("disabled_extension_paths", JsonArray(emptyList()))
            put("disabled_package_sources", JsonArray(emptyList()))
            put("host_tools", hostToolExecutor?.definitions ?: JsonArray(emptyList()))
        }
        val response = bridge.request(
            type = "run_turn",
            payload = payload,
            onEvent = { event, eventPayload ->
                when (event) {
                    "assistant_text_delta" -> onAssistantTextDelta(eventPayload.string("delta"))
                    "assistant_reasoning_delta" -> onAssistantReasoningDelta(eventPayload.string("delta"))
                    "host_tool_request" -> executeHostTool(
                        request = eventPayload,
                        onStarted = onHostToolStarted,
                        onFinished = onHostToolFinished,
                    )
                }
            },
        )
        val usage = response["usage"] as? JsonObject ?: JsonObject(emptyMap())
        return SharedPiTurnResult(
            assistantText = response.string("assistant_text"),
            reasoningText = response.string("reasoning_text"),
            provider = response.string("provider"),
            model = response.string("model"),
            errorMessage = response.string("error_message"),
            usage = SharedPiUsage(
                inputTokens = usage.long("input_tokens"),
                outputTokens = usage.long("output_tokens"),
                totalTokens = usage.long("total_tokens"),
                reasoningTokens = usage.long("reasoning_tokens"),
                cachedInputTokens = usage.long("cached_input_tokens"),
            ),
        )
    }

    private suspend fun executeHostTool(
        request: JsonObject,
        onStarted: suspend (SharedPiHostToolCall) -> Unit,
        onFinished: suspend (SharedPiHostToolCall, SharedHostToolResult) -> Unit,
    ) {
        val executor = hostToolExecutor ?: return
        val toolName = request.string("tool_name")
        val arguments = request["arguments"] as? JsonObject ?: JsonObject(emptyMap())
        val call = SharedPiHostToolCall(
            id = request.string("tool_call_id").ifBlank { request.string("tool_request_id") },
            name = toolName,
            arguments = arguments,
        )
        onStarted(call)
        val result = executor.execute(toolName, arguments)
        onFinished(call, result)
        bridge.request(
            type = "host_tool_result",
            payload = buildJsonObject {
                put("tool_request_id", request.string("tool_request_id"))
                put("session_id", request.string("session_id"))
                put("tool_call_id", request.string("tool_call_id"))
                put("tool_name", toolName)
                put("arguments_json", request.string("arguments_json"))
                put("output_json", result.outputJson)
                put("is_error", result.isError)
            },
            timeoutMillis = 15_000,
            abortOnCancellation = false,
        )
    }
}

data class SharedPiHostToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject,
)

fun LlmProviderConfig.toSharedPiModelConfig(): JsonObject {
    val definition = PiProviderCatalog.resolve(piProviderId)
    val effectiveAuthMethod = if (
        authMethod == ProviderAuthMethod.ApiKey &&
        !definition.supportsApiKey &&
        definition.supportsAmbientAuth
    ) {
        ProviderAuthMethod.Ambient
    } else {
        authMethod
    }
    val resolvedPiProviderId = if (definition.isBuiltIn) {
        definition.id
    } else {
        "aether-${stableProviderSuffix(providerId.ifBlank { baseUrl })}"
    }
    return buildJsonObject {
        put("provider_type", if (definition.isBuiltIn) "builtin" else "custom")
        put("provider_config_id", id)
        put("pi_provider_id", resolvedPiProviderId)
        put("pi_api", if (definition.isBuiltIn) "builtin" else "openai-completions")
        put("model_id", modelId.trim())
        put("base_url", baseUrl.trim())
        put("api_key", if (effectiveAuthMethod == ProviderAuthMethod.ApiKey) apiKey.trim() else "")
        put("custom_headers", buildJsonObject {
            customHeaders.forEach { header ->
                header.name.trim().takeIf(String::isNotBlank)?.let { put(it, header.value) }
            }
            put("User-Agent", normalizeLlmUserAgent(userAgent))
        })
        put("reasoning", false)
        put("context_window", 128_000)
        put("max_tokens", 16_384)
        put("timeout_ms", 360_000)
        put("max_retries", 2)
        put("max_retry_delay_ms", 60_000)
        put("auth_method", effectiveAuthMethod.storageValue)
        if (oauthCredentialJson.isNotBlank()) {
            val credential = runCatching {
                Json.parseToJsonElement(oauthCredentialJson) as? JsonObject
            }.getOrNull()
            if (credential != null) put("oauth_credential", credential)
        }
        put("provider_env", buildJsonObject {
            providerEnvironmentVariables.forEach { variable ->
                variable.name.trim().takeIf(String::isNotBlank)?.let { put(it, variable.value) }
            }
        })
    }
}

private fun List<SharedPiChatMessage>.toPiMessages(): JsonArray = buildJsonArray {
    this@toPiMessages.forEach { message ->
        add(message.toPiMessage())
    }
}

private fun SharedPiChatMessage.toPiMessage(): JsonObject = buildJsonObject {
    put("role", role)
    put("text", text)
    if (images.isEmpty()) {
        put("content", text)
    } else {
        put("content", buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", this@toPiMessage.text)
            })
            images.forEach { image ->
                add(buildJsonObject {
                    put("type", "image")
                    put("mime_type", image.mimeType)
                    put("data", image.data)
                })
            }
        })
    }
}

private fun stableProviderSuffix(value: String): String = value
    .trim()
    .lowercase()
    .ifBlank { "custom" }
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
    .take(48)
    .ifBlank { "custom" }

private fun JsonObject.string(name: String): String =
    get(name)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.long(name: String): Long =
    get(name)?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0
