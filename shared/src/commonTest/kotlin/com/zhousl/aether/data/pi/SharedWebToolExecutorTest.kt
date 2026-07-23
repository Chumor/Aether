package com.zhousl.aether.data.pi

import com.zhousl.aether.data.AppSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class SharedWebToolExecutorTest {
    @Test
    fun fetchNormalizesUrlAndReturnsReadableHtml() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/page", request.url.toString())
            respond(
                content = "<html><body><main><h1>Aether</h1><p>Shared page</p><script>ignore()</script></main></body></html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
            )
        }
        val executor = SharedWebToolExecutor({ AppSettings() }, engine)

        val result = executor.execute(
            "web_fetch",
            JsonObject(mapOf("url" to JsonPrimitive("example.com/page"))),
        )

        assertFalse(result.isError)
        assertContains(result.outputJson, "Aether")
        assertContains(result.outputJson, "Shared page")
        assertFalse(result.outputJson.contains("ignore()"))
    }

    @Test
    fun searchUsesConfiguredTavilyEndpointAndBearerToken() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://search.example/v1/search", request.url.toString())
            assertEquals("Bearer secret", request.headers[HttpHeaders.Authorization])
            assertContains(request.body.toString(), "Kotlin Multiplatform")
            respond(
                content = "{\"answer\":\"Compose\",\"results\":[]}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val executor = SharedWebToolExecutor(
            settings = { AppSettings(tavilyApiKey = "secret", tavilyBaseUrl = "https://search.example/v1") },
            engine = engine,
        )

        val result = executor.execute(
            "web_search",
            JsonObject(mapOf("query" to JsonPrimitive("Kotlin Multiplatform"))),
        )

        assertFalse(result.isError)
        assertContains(result.outputJson, "Compose")
        assertContains(result.outputJson, "\"ok\":true")
    }

    @Test
    fun searchRejectsMissingApiKeyBeforeNetworkCall() = runTest {
        var called = false
        val engine = MockEngine {
            called = true
            respond("{}")
        }
        val executor = SharedWebToolExecutor({ AppSettings(tavilyApiKey = "") }, engine)

        val result = executor.execute(
            "web_search",
            JsonObject(mapOf("query" to JsonPrimitive("Aether"))),
        )

        assertTrue(result.isError)
        assertContains(result.outputJson, "not configured")
        assertFalse(called)
    }
}
