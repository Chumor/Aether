package com.zhousl.aether.data.pi

import com.zhousl.aether.data.AppSettings
import com.zhousl.aether.data.normalizeTavilyBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val DefaultFetchLimit = 20_000
private const val MaximumFetchLimit = 100_000

class SharedWebToolExecutor(
    private val settings: () -> AppSettings,
    engine: HttpClientEngine? = null,
) : SharedHostToolExecutor {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = if (engine == null) HttpClient {
        install(ContentNegotiation) { json(json) }
    } else HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
    }

    override val definitions: JsonArray = buildJsonArray {
        add(toolDefinition(
            name = "web_fetch",
            description = "Fetch an HTTP or HTTPS page and return readable text or structured content.",
            required = listOf("url"),
            properties = mapOf("url" to "string", "max_chars" to "integer"),
        ))
        add(toolDefinition(
            name = "web_search",
            description = "Search the web with Tavily. Requires a Tavily API key in Web Tools settings.",
            required = listOf("query"),
            properties = mapOf("query" to "string", "max_results" to "integer", "search_depth" to "string"),
        ))
    }

    override suspend fun execute(name: String, arguments: JsonObject): SharedHostToolResult =
        runCatching {
            when (name) {
                "web_fetch" -> fetch(arguments)
                "web_search" -> search(arguments)
                else -> error("Unsupported web tool: $name")
            }
        }.getOrElse { error ->
            SharedHostToolResult(
                outputJson = buildJsonObject {
                    put("ok", false)
                    put("error", error.message ?: "Web tool failed.")
                }.toString(),
                isError = true,
            )
        }

    private suspend fun fetch(arguments: JsonObject): SharedHostToolResult {
        val rawUrl = arguments.string("url")
        require(rawUrl.isNotBlank()) { "web_fetch.url is required." }
        val url = normalizedHttpUrl(rawUrl)
        val limit = (arguments.int("max_chars") ?: DefaultFetchLimit).coerceIn(500, MaximumFetchLimit)
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, "Aether/1.0 (Compose Multiplatform)")
            accept(ContentType.Any)
        }
        val body = response.body<String>()
        check(response.status.isSuccess()) { "HTTP ${response.status.value} while fetching $url." }
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty()
        val readable = if (contentType.contains("html", ignoreCase = true) || looksLikeHtml(body)) {
            htmlToReadableText(body)
        } else body.trim()
        val truncated = readable.length > limit
        val output = if (truncated) readable.take(limit).trimEnd() + "\n\n...[truncated]" else readable
        return SharedHostToolResult(buildJsonObject {
            put("ok", true)
            put("request_url", url)
            put("final_url", response.call.request.url.toString())
            put("content_type", contentType)
            put("content", output)
            put("truncated", truncated)
        }.toString())
    }

    private suspend fun search(arguments: JsonObject): SharedHostToolResult {
        val query = arguments.string("query").trim()
        require(query.isNotBlank()) { "web_search.query is required." }
        val current = settings()
        val apiKey = current.tavilyApiKey.trim()
        require(apiKey.isNotBlank()) { "Tavily API key is not configured." }
        val endpoint = tavilySearchEndpoint(current.tavilyBaseUrl)
        val response = client.post(endpoint) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("query", query)
                put("topic", arguments.string("topic").ifBlank { "general" })
                put("search_depth", arguments.string("search_depth").ifBlank { "basic" })
                put("max_results", (arguments.int("max_results") ?: 5).coerceIn(1, 20))
                put("include_answer", "basic")
                put("include_favicon", true)
                put("include_usage", true)
            })
        }
        val body = response.body<String>()
        check(response.status.isSuccess()) {
            val message = runCatching {
                (json.parseToJsonElement(body) as? JsonObject)?.string("detail")
            }.getOrNull().orEmpty()
            message.ifBlank { "HTTP ${response.status.value} from Tavily." }
        }
        val payload = json.parseToJsonElement(body) as? JsonObject
            ?: error("Tavily returned non-JSON content.")
        return SharedHostToolResult(
            JsonObject(payload + ("ok" to JsonPrimitive(true))).toString()
        )
    }
}

private fun toolDefinition(
    name: String,
    description: String,
    required: List<String>,
    properties: Map<String, String>,
): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    put("execution_mode", "parallel")
    put("parameters", buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            properties.forEach { (property, type) ->
                put(property, buildJsonObject { put("type", type) })
            }
        })
        put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
        put("additionalProperties", false)
    })
}

private fun normalizedHttpUrl(value: String): String {
    val candidate = value.trim().let { if ("://" in it) it else "https://$it" }
    val url = Url(candidate)
    require(url.protocol.name == "http" || url.protocol.name == "https") {
        "URL must use HTTP or HTTPS."
    }
    require(url.host.isNotBlank()) { "URL must include a host." }
    return url.toString()
}

internal fun tavilySearchEndpoint(baseUrl: String): String {
    val normalized = normalizeTavilyBaseUrl(baseUrl).trimEnd('/')
    return if (normalized.endsWith("/search")) normalized else "$normalized/search"
}

private fun looksLikeHtml(value: String): Boolean = value.trimStart().let {
    it.startsWith("<!doctype", ignoreCase = true) || it.startsWith("<html", ignoreCase = true)
}

internal fun htmlToReadableText(html: String): String = html
    .replace(Regex("(?is)<(script|style|noscript|svg|canvas|iframe)[^>]*>.*?</\\1>"), "")
    .replace(Regex("(?i)<br\\s*/?>"), "\n")
    .replace(Regex("(?i)</(p|div|section|article|main|h[1-6]|li|tr)>"), "\n")
    .replace(Regex("(?s)<[^>]+>"), "")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace(Regex("[ \\t]+"), " ")
    .replace(Regex("\\n[ \\t]+"), "\n")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()

private fun JsonObject.string(name: String): String = get(name)?.jsonPrimitive?.contentOrNull.orEmpty()
private fun JsonObject.int(name: String): Int? = get(name)?.jsonPrimitive?.intOrNull
