package com.zhousl.aether.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class SharedProviderModelsResult(
    val models: List<String>,
    val error: String? = null,
)

class SharedProviderModelCatalogClient(engine: HttpClientEngine? = null) {
    private val client = if (engine == null) createClient() else createClient(engine)

    suspend fun fetchModels(
        config: LlmProviderConfig,
        fetchBuiltinCatalog: suspend () -> JsonObject,
    ): SharedProviderModelsResult {
        val definition = PiProviderCatalog.resolve(config.piProviderId)
        return if (shouldFetchModelsFromEndpoint(config, definition)) {
            fetchOpenAiModels(config)
        } else {
            val models = modelsFromProviderCatalog(fetchBuiltinCatalog(), definition.id)
            SharedProviderModelsResult(
                models = models,
                error = if (models.isEmpty()) {
                    "Provider ${definition.id} is unavailable in the Pi model catalog."
                } else null,
            )
        }
    }

    private suspend fun fetchOpenAiModels(config: LlmProviderConfig): SharedProviderModelsResult {
        val modelsUrl = modelsEndpoint(config.baseUrl)
        return runCatching {
            val response = client.get(modelsUrl) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${config.apiKey.trim()}")
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.UserAgent, normalizeLlmUserAgent(config.userAgent))
                    config.customHeaders.normalizedLlmHeaders().forEach { header ->
                        remove(header.name)
                        append(header.name, header.value)
                    }
                }
            }
            val body = response.body<String>()
            if (!response.status.isSuccess()) {
                return@runCatching SharedProviderModelsResult(
                    emptyList(),
                    body.ifBlank { "HTTP ${response.status.value}" },
                )
            }
            val root = Json.parseToJsonElement(body) as? JsonObject
                ?: return@runCatching SharedProviderModelsResult(emptyList(), "Invalid model response.")
            val models = (root["data"] as? JsonArray)
                ?.mapNotNull { entry ->
                    (entry as? JsonObject)
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                }
                ?.distinctBy(String::lowercase)
                .orEmpty()
            SharedProviderModelsResult(
                models,
                if (models.isEmpty()) "The model endpoint returned no models." else null,
            )
        }.getOrElse { error ->
            SharedProviderModelsResult(emptyList(), error.message ?: "Unable to fetch models.")
        }
    }

    private companion object {
        fun createClient(engine: HttpClientEngine? = null): HttpClient =
            if (engine == null) {
                HttpClient { configureTimeouts() }
            } else {
                HttpClient(engine) { configureTimeouts() }
            }

        fun io.ktor.client.HttpClientConfig<*>.configureTimeouts() {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
                requestTimeoutMillis = 30_000
            }
        }
    }
}

internal fun shouldFetchModelsFromEndpoint(
    config: LlmProviderConfig,
    definition: PiProviderDefinition = PiProviderCatalog.resolve(config.piProviderId),
): Boolean {
    if (!definition.isBuiltIn) return true
    if (definition.id == "openai" && config.authMethod == ProviderAuthMethod.ApiKey) return true
    val normalizedBaseUrl = config.baseUrl.trim().trimEnd('/')
    return definition.id == "openai" &&
        normalizedBaseUrl.isNotBlank() &&
        normalizedBaseUrl != definition.defaultBaseUrl
}

internal fun modelsEndpoint(baseUrl: String): String {
    val normalized = baseUrl.trim().trimEnd('/')
    require(normalized.isNotBlank()) { "A base URL is required to fetch models." }
    return when {
        normalized.endsWith("/responses") -> normalized.removeSuffix("/responses") + "/models"
        normalized.endsWith("/chat/completions") -> normalized.removeSuffix("/chat/completions") + "/models"
        else -> "$normalized/models"
    }
}

internal fun modelsFromProviderCatalog(catalog: JsonObject, providerId: String): List<String> {
    val providers = catalog["providers"] as? JsonArray ?: return emptyList()
    val provider = providers
        .mapNotNull { it as? JsonObject }
        .firstOrNull { entry -> entry["id"]?.jsonPrimitive?.contentOrNull == providerId }
        ?: return emptyList()
    return (provider["models"] as? JsonArray)
        ?.mapNotNull { model ->
            (model as? JsonObject)
                ?.get("id")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                ?.takeIf(String::isNotBlank)
        }
        ?.distinct()
        .orEmpty()
}
