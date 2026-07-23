package com.zhousl.aether.data.pi

import com.zhousl.aether.data.platformRandomUuid
import com.zhousl.aether.runtime.MultiplatformLocalRuntime
import com.zhousl.aether.runtime.RuntimeProcessSpec
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

enum class SharedMcpTransport { Http, Stdio }

data class SharedMcpServerConfig(
    val id: String = platformRandomUuid(),
    val name: String,
    val transport: SharedMcpTransport,
    val url: String = "",
    val command: String = "",
    val arguments: List<String> = emptyList(),
    val enabled: Boolean = true,
)

data class SharedMcpToolBinding(
    val exposedName: String,
    val serverId: String,
    val remoteName: String,
    val description: String,
    val inputSchema: JsonObject,
)

class SharedMcpManager(
    private val runtime: MultiplatformLocalRuntime,
) {
    private val configPath = "${runtime.homeDirectory}/.aether/mcp-servers.json"
    private val clientPath = "${runtime.homeDirectory}/.aether/mcp-client.mjs"
    private var bindings: List<SharedMcpToolBinding> = emptyList()

    suspend fun loadServers(): List<SharedMcpServerConfig> {
        if (!runtime.fileSystem.exists(configPath)) return emptyList()
        return parseSharedMcpServers(runtime.fileSystem.read(configPath).decodeToString())
    }

    suspend fun saveServers(servers: List<SharedMcpServerConfig>) {
        runtime.fileSystem.createDirectories(configPath.substringBeforeLast('/'))
        runtime.fileSystem.write(
            configPath,
            serializeSharedMcpServers(servers).encodeToByteArray(),
        )
    }

    suspend fun refreshBindings(servers: List<SharedMcpServerConfig>? = null): List<SharedMcpToolBinding> {
        val resolvedServers = servers ?: loadServers()
        val discovered = mutableListOf<SharedMcpToolBinding>()
        for (server in resolvedServers.filter(SharedMcpServerConfig::enabled)) {
            val response = runClient("list", server, null)
            val tools = (response["tools"] as? JsonArray).orEmpty()
            tools.mapNotNullTo(discovered) { element ->
                val tool = element as? JsonObject ?: return@mapNotNullTo null
                val remoteName = tool.string("name")
                if (remoteName.isBlank()) return@mapNotNullTo null
                SharedMcpToolBinding(
                    exposedName = sharedMcpToolName(server.name.ifBlank { server.id }, remoteName),
                    serverId = server.id,
                    remoteName = remoteName,
                    description = tool.string("description"),
                    inputSchema = tool["inputSchema"] as? JsonObject ?: JsonObject(emptyMap()),
                )
            }
        }
        bindings = discovered
        return discovered
    }

    fun definitions(): JsonArray = buildJsonArray {
        bindings.forEach { binding ->
            add(buildJsonObject {
                put("name", binding.exposedName)
                put("description", binding.description.ifBlank { "MCP tool ${binding.remoteName}" })
                put("execution_mode", "parallel")
                put("parameters", binding.inputSchema)
            })
        }
    }

    suspend fun execute(exposedName: String, arguments: JsonObject): SharedHostToolResult {
        val binding = bindings.firstOrNull { it.exposedName == exposedName }
            ?: return SharedHostToolResult("{\"error\":\"Unknown MCP tool\"}", true)
        val server = loadServers().firstOrNull { it.id == binding.serverId && it.enabled }
            ?: return SharedHostToolResult("{\"error\":\"MCP server is disabled\"}", true)
        return runCatching {
            val response = runClient(
                "call",
                server,
                buildJsonObject {
                    put("name", binding.remoteName)
                    put("arguments", arguments)
                },
            )
            SharedHostToolResult(
                outputJson = response.toString(),
                isError = response["isError"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true,
            )
        }.getOrElse { error ->
            SharedHostToolResult(
                buildJsonObject { put("error", error.message ?: "MCP call failed") }.toString(),
                true,
            )
        }
    }

    private suspend fun runClient(action: String, server: SharedMcpServerConfig, call: JsonObject?): JsonObject = coroutineScope {
        ensureClientInstalled()
        val process = runtime.startProcess(
            RuntimeProcessSpec(
                executable = "/usr/bin/node",
                arguments = listOf(clientPath),
                workingDirectory = runtime.workspaceRoot,
                environment = mapOf("HOME" to runtime.homeDirectory),
            )
        )
        val stdout = async { process.stdout.toList().flattenMcpBytes().decodeToString() }
        val stderr = async { process.stderr.toList().flattenMcpBytes().decodeToString() }
        process.writeStdin(
            buildJsonObject {
                put("action", action)
                put("server", server.toJson())
                if (call != null) put("call", call)
            }.toString().encodeToByteArray()
        )
        process.closeStdin()
        val exit = process.awaitExit()
        val output = stdout.await().trim()
        val errors = stderr.await().trim()
        check(exit.exitCode == 0) { errors.ifBlank { "MCP client exited with ${exit.exitCode}." } }
        Json.parseToJsonElement(output).jsonObject
    }

    private suspend fun ensureClientInstalled() {
        if (runtime.fileSystem.exists(clientPath)) return
        runtime.fileSystem.createDirectories(clientPath.substringBeforeLast('/'))
        runtime.fileSystem.write(clientPath, SharedMcpNodeClient.encodeToByteArray())
    }
}

class SharedToolRegistry(
    private val runtimeTools: SharedHostToolExecutor,
    private val mcp: SharedMcpManager,
) : SharedHostToolExecutor {
    override val definitions: JsonArray
        get() = JsonArray(runtimeTools.definitions + mcp.definitions())

    override suspend fun execute(name: String, arguments: JsonObject): SharedHostToolResult =
        if (name.startsWith("mcp__")) mcp.execute(name, arguments) else runtimeTools.execute(name, arguments)
}

internal fun SharedMcpServerConfig.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("name", name)
    put("transport", transport.name.lowercase())
    put("url", url)
    put("command", command)
    put("arguments", buildJsonArray { arguments.forEach { add(JsonPrimitive(it)) } })
}

internal fun sharedMcpToolName(serverName: String, remoteName: String): String =
    "mcp__${safeMcpName(serverName)}__${safeMcpName(remoteName)}"

private fun safeMcpName(value: String): String =
    value.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "unnamed" }

internal fun serializeSharedMcpServers(servers: List<SharedMcpServerConfig>): String =
    buildJsonArray {
        servers.forEach { server ->
            add(JsonObject(server.toJson() + ("enabled" to JsonPrimitive(server.enabled))))
        }
    }.toString()

internal fun parseSharedMcpServers(value: String): List<SharedMcpServerConfig> = runCatching {
    Json.parseToJsonElement(value).jsonArray.mapNotNull { element ->
        val item = element as? JsonObject ?: return@mapNotNull null
        SharedMcpServerConfig(
            id = item.string("id"),
            name = item.string("name"),
            transport = if (item.string("transport") == "stdio") SharedMcpTransport.Stdio else SharedMcpTransport.Http,
            url = item.string("url"),
            command = item.string("command"),
            arguments = (item["arguments"] as? JsonArray).orEmpty().mapNotNull {
                it.jsonPrimitive.contentOrNull
            },
            enabled = item["enabled"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true,
        )
    }
}.getOrDefault(emptyList())

private fun JsonObject.string(name: String): String = get(name)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun List<ByteArray>.flattenMcpBytes(): ByteArray {
    val result = ByteArray(sumOf(ByteArray::size))
    var offset = 0
    forEach { bytes -> bytes.copyInto(result, offset).also { offset += bytes.size } }
    return result
}

private val SharedMcpNodeClient = """
import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
const input = await new Promise((resolve, reject) => {
  const chunks = []; process.stdin.on('data', c => chunks.push(c));
  process.stdin.on('end', () => resolve(Buffer.concat(chunks).toString('utf8'))); process.stdin.on('error', reject);
});
const request = JSON.parse(input); const server = request.server; let nextId = 1;
const parseHttp = async (response) => {
  const text = await response.text();
  if (!response.ok) throw new Error(`HTTP ${'$'}{response.status}: ${'$'}{text}`);
  if ((response.headers.get('content-type') || '').includes('text/event-stream')) {
    const lines = text.split(/\r?\n/).filter(x => x.startsWith('data:'));
    return JSON.parse(lines.at(-1).slice(5).trim());
  }
  return JSON.parse(text);
};
async function httpClient() {
  let session = '';
  const send = async (method, params, notification = false) => {
    const payload = {jsonrpc:'2.0', method, ...(params ? {params} : {}), ...(notification ? {} : {id: nextId++})};
    const response = await fetch(server.url, {method:'POST', headers:{'content-type':'application/json','accept':'application/json, text/event-stream', ...(session ? {'mcp-session-id':session} : {})}, body:JSON.stringify(payload)});
    session ||= response.headers.get('mcp-session-id') || '';
    return notification ? {} : parseHttp(response);
  };
  return {send, close: async () => {}};
}
async function stdioClient() {
  const child = spawn(server.command, server.arguments || [], {stdio:['pipe','pipe','pipe'], env:{...process.env}});
  let stderr = ''; child.stderr.on('data', c => stderr += c.toString());
  const pending = new Map();
  createInterface({input:child.stdout}).on('line', line => { try { const msg=JSON.parse(line); if (msg.id != null && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); } } catch {} });
  const send = async (method, params, notification = false) => {
    const id = nextId++; const payload={jsonrpc:'2.0',method,...(params?{params}:{}),...(notification?{}:{id})};
    child.stdin.write(JSON.stringify(payload)+'\n'); if (notification) return {};
    return await new Promise((resolve,reject) => { pending.set(id,resolve); setTimeout(() => { if(pending.delete(id)) reject(new Error(`MCP stdio timeout: ${'$'}{stderr}`)); },30000); });
  };
  return {send, close: async () => { child.kill('SIGTERM'); }};
}
const client = server.transport === 'stdio' ? await stdioClient() : await httpClient();
try {
  const initialized = await client.send('initialize',{protocolVersion:'2025-03-26',capabilities:{},clientInfo:{name:'Aether',version:'1'}});
  if (initialized.error) throw new Error(initialized.error.message || 'MCP initialize failed');
  await client.send('notifications/initialized',{},true);
  const response = request.action === 'list'
    ? await client.send('tools/list',{})
    : await client.send('tools/call',{name:request.call.name,arguments:request.call.arguments || {}});
  if (response.error) throw new Error(response.error.message || 'MCP request failed');
  process.stdout.write(JSON.stringify(response.result || {}));
} finally { await client.close(); }
""".trimIndent()
