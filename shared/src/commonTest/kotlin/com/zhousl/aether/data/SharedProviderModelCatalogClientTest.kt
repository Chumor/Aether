package com.zhousl.aether.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class SharedProviderModelCatalogClientTest {
    @Test
    fun customProviderFetchesConfiguredModelsEndpointWithHeaders() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://models.example/v1/models", request.url.toString())
            assertEquals("Bearer secret", request.headers[HttpHeaders.Authorization])
            assertEquals("Aether-Test", request.headers[HttpHeaders.UserAgent])
            assertEquals("tenant-1", request.headers["X-Tenant"])
            respond(
                content = """{"data":[{"id":"model-a"},{"id":"MODEL-A"},{"id":"model-b"}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val result = SharedProviderModelCatalogClient(engine).fetchModels(
            customConfig(),
            fetchBuiltinCatalog = { error("Built-in catalog should not be used") },
        )

        assertEquals(listOf("model-a", "model-b"), result.models)
        assertNull(result.error)
    }

    @Test
    fun builtInProviderUsesPiCatalogWithoutCallingNetwork() = runTest {
        var networkCalled = false
        val engine = MockEngine {
            networkCalled = true
            respond("{}")
        }
        val catalog = Json.parseToJsonElement(
            """{"providers":[{"id":"anthropic","models":[{"id":"claude-a","reasoning":true,"thinking_levels":["off","low","high","unknown"],"thinking_level_clamps":{"max":"high","invalid":"low"}},{"id":"claude-b"}]}]}""",
        ) as JsonObject
        val result = SharedProviderModelCatalogClient(engine).fetchModels(
            customConfig(
                piProviderId = "anthropic",
                baseUrl = "https://api.anthropic.com",
                authMethod = ProviderAuthMethod.OAuth,
            ),
            fetchBuiltinCatalog = { catalog },
        )

        assertEquals(listOf("claude-a", "claude-b"), result.models)
        assertEquals(listOf("off", "low", "high"), result.thinkingLevelsByModel["claude-a"])
        assertEquals(mapOf("max" to "high"), result.thinkingLevelClampsByModel["claude-a"])
        assertFalse(networkCalled)
    }

    @Test
    fun openAiApiKeyUsesRemoteEndpoint() {
        assertTrue(
            shouldFetchModelsFromEndpoint(
                customConfig(
                    piProviderId = "openai",
                    baseUrl = "https://api.openai.com/v1",
                    authMethod = ProviderAuthMethod.ApiKey,
                ),
            ),
        )
        assertEquals("https://example.com/v1/models", modelsEndpoint("https://example.com/v1/responses"))
        assertEquals("https://example.com/v1/models", modelsEndpoint("https://example.com/v1/chat/completions"))
    }
}

private fun customConfig(
    piProviderId: String = DefaultPiProviderId,
    baseUrl: String = "https://models.example/v1",
    authMethod: ProviderAuthMethod = ProviderAuthMethod.ApiKey,
) = LlmProviderConfig(
    providerId = "test-provider",
    name = "Test",
    piProviderId = piProviderId,
    apiKey = "secret",
    baseUrl = baseUrl,
    authMethod = authMethod,
    modelId = "model-a",
    userAgent = "Aether-Test",
    customHeaders = listOf(LlmCustomHeader("X-Tenant", "tenant-1")),
)
