package com.zhousl.aether.data.pi

import com.zhousl.aether.runtime.MultiplatformLocalRuntime
import com.zhousl.aether.runtime.RuntimeProcessSpec
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class SharedHostToolResult(
    val outputJson: String,
    val isError: Boolean = false,
)

interface SharedHostToolExecutor {
    val definitions: JsonArray
    suspend fun execute(name: String, arguments: JsonObject): SharedHostToolResult
}

class RuntimeHostToolExecutor(
    private val runtime: MultiplatformLocalRuntime,
) : SharedHostToolExecutor {
    override val definitions: JsonArray = sharedRuntimeHostToolDefinitions()

    override suspend fun execute(name: String, arguments: JsonObject): SharedHostToolResult =
        runCatching {
            when (name) {
                "read" -> read(arguments)
                "write" -> write(arguments)
                "edit" -> edit(arguments)
                "ls" -> shell(arguments, listCommand(arguments))
                "find" -> shell(arguments, findCommand(arguments))
                "grep" -> shell(arguments, grepCommand(arguments))
                "bash" -> shell(arguments, arguments.string("command"))
                "sleep" -> shell(
                    arguments,
                    "sleep ${((arguments.int("duration_ms") ?: 1_000).coerceIn(0, 60_000)) / 1_000.0}",
                )
                else -> error("Unsupported shared host tool: $name")
            }
        }.getOrElse { error ->
            SharedHostToolResult(
                outputJson = buildJsonObject {
                    put("ok", false)
                    put("error", error.message ?: "Host tool failed.")
                }.toString(),
                isError = true,
            )
        }

    private suspend fun read(arguments: JsonObject): SharedHostToolResult {
        val path = resolvePath(arguments)
        val lines = runtime.fileSystem.read(path).decodeToString().lines()
        val offset = (arguments.int("offset") ?: 0).coerceAtLeast(0)
        val limit = (arguments.int("limit") ?: 2_000).coerceIn(1, 10_000)
        val selected = lines.drop(offset).take(limit)
        val showNumbers = arguments.boolean("showLineNumbers") || arguments.boolean("show_line_numbers")
        val output = if (showNumbers) {
            selected.mapIndexed { index, line -> "${offset + index + 1}: $line" }.joinToString("\n")
        } else {
            selected.joinToString("\n")
        }
        return SharedHostToolResult(
            buildJsonObject {
                put("ok", true)
                put("path", path)
                put("stdout", output)
                put("truncated", lines.size > offset + selected.size)
            }.toString()
        )
    }

    private suspend fun write(arguments: JsonObject): SharedHostToolResult {
        val path = resolvePath(arguments)
        val content = arguments.string("content")
        path.substringBeforeLast('/', "").takeIf(String::isNotBlank)?.let {
            runtime.fileSystem.createDirectories(it)
        }
        runtime.fileSystem.write(path, content.encodeToByteArray())
        return SharedHostToolResult(
            buildJsonObject {
                put("ok", true)
                put("path", path)
                put("bytes_written", content.encodeToByteArray().size)
            }.toString()
        )
    }

    private suspend fun edit(arguments: JsonObject): SharedHostToolResult {
        val path = resolvePath(arguments)
        val oldText = arguments.string("oldText")
        val newText = arguments.string("newText")
        require(oldText.isNotEmpty()) { "edit.oldText is required." }
        val current = runtime.fileSystem.read(path).decodeToString()
        val occurrences = current.windowed(oldText.length, 1).count { it == oldText }
        require(occurrences == 1) { "edit.oldText must match exactly once; found $occurrences matches." }
        runtime.fileSystem.write(path, current.replace(oldText, newText).encodeToByteArray())
        return SharedHostToolResult(
            buildJsonObject {
                put("ok", true)
                put("path", path)
                put("replacements", 1)
            }.toString()
        )
    }

    private suspend fun shell(arguments: JsonObject, command: String): SharedHostToolResult = coroutineScope {
        require(command.isNotBlank()) { "A command is required." }
        val workingDirectory = resolveWorkingDirectory(arguments)
        val process = runtime.startProcess(
            RuntimeProcessSpec(
                executable = "/bin/sh",
                arguments = listOf("-lc", command),
                environment = mapOf(
                    "HOME" to runtime.homeDirectory,
                    "AETHER_WORKSPACE" to runtime.workspaceRoot,
                ),
                workingDirectory = workingDirectory,
            )
        )
        process.closeStdin()
        val stdout = async { process.stdout.toList().flattenBytes().decodeToStringLimited() }
        val stderr = async { process.stderr.toList().flattenBytes().decodeToStringLimited() }
        val exit = process.awaitExit()
        val stdoutText = stdout.await()
        val stderrText = stderr.await()
        SharedHostToolResult(
            outputJson = buildJsonObject {
                put("ok", exit.exitCode == 0)
                put("status", "completed")
                put("exit_code", exit.exitCode)
                put("stdout", stdoutText)
                put("stderr", stderrText)
            }.toString(),
            isError = exit.exitCode != 0,
        )
    }

    private fun listCommand(arguments: JsonObject): String {
        val path = resolvePath(arguments)
        val recursive = arguments.boolean("recursive")
        val maxDepth = (arguments.int("maxDepth") ?: if (recursive) 8 else 1).coerceIn(1, 20)
        val hidden = if (arguments.boolean("includeHidden")) "" else " ! -path '*/.*'"
        return "find ${path.shellQuote()} -maxdepth $maxDepth -mindepth 1$hidden -print | head -2000"
    }

    private fun findCommand(arguments: JsonObject): String {
        val path = resolvePath(arguments)
        val pattern = arguments.string("pattern").ifBlank { "*" }
        val maxDepth = (arguments.int("maxDepth") ?: 8).coerceIn(1, 20)
        return "find ${path.shellQuote()} -maxdepth $maxDepth -name ${pattern.shellQuote()} -print | head -2000"
    }

    private fun grepCommand(arguments: JsonObject): String {
        val path = resolvePath(arguments)
        val pattern = arguments.string("pattern")
        require(pattern.isNotBlank()) { "grep.pattern is required." }
        val caseFlag = if (arguments.boolean("caseSensitive")) "" else " -i"
        val regexFlag = if (arguments.boolean("isRegex")) " -E" else " -F"
        return "grep -R -n$caseFlag$regexFlag -- ${pattern.shellQuote()} ${path.shellQuote()} | head -2000"
    }

    private fun resolvePath(arguments: JsonObject): String {
        val raw = arguments.string("path").ifBlank { "." }
        return normalizeGuestPath(raw, resolveWorkingDirectory(arguments))
    }

    private fun resolveWorkingDirectory(arguments: JsonObject): String {
        val raw = arguments.string("working_directory")
            .ifBlank { arguments.string("workingDirectory") }
            .ifBlank { runtime.workspaceRoot }
        return normalizeGuestPath(raw, runtime.workspaceRoot)
    }

    private fun normalizeGuestPath(rawPath: String, workingDirectory: String): String {
        val expanded = when {
            rawPath == "~" -> runtime.homeDirectory
            rawPath.startsWith("~/") -> runtime.homeDirectory + rawPath.removePrefix("~")
            rawPath.startsWith('/') -> rawPath
            else -> "${workingDirectory.trimEnd('/')}/$rawPath"
        }
        val parts = mutableListOf<String>()
        expanded.split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts += part
            }
        }
        return "/" + parts.joinToString("/")
    }
}

private fun sharedRuntimeHostToolDefinitions(): JsonArray = buildJsonArray {
    add(toolDefinition("read", "Read a text file from Alpine.", "parallel", "path" to "string"))
    add(toolDefinition("write", "Create or overwrite a text file in Alpine.", "sequential", "path" to "string", "content" to "string"))
    add(toolDefinition("edit", "Replace one exact text occurrence in an Alpine file.", "sequential", "path" to "string", "oldText" to "string", "newText" to "string"))
    add(toolDefinition("ls", "List files and directories in Alpine.", "parallel", "path" to "string"))
    add(toolDefinition("find", "Find files by glob pattern in Alpine.", "parallel", "path" to "string", "pattern" to "string"))
    add(toolDefinition("grep", "Search text in Alpine files.", "parallel", "path" to "string", "pattern" to "string"))
    add(toolDefinition("bash", "Execute a shell command in Alpine.", "sequential", "command" to "string"))
    add(toolDefinition("sleep", "Pause briefly before continuing.", "sequential", "duration_ms" to "integer"))
}

private fun toolDefinition(
    name: String,
    description: String,
    executionMode: String,
    vararg properties: Pair<String, String>,
): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    put("execution_mode", executionMode)
    put("parameters", buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            properties.forEach { (property, type) ->
                put(property, buildJsonObject { put("type", type) })
            }
        })
        put("required", buildJsonArray { properties.forEach { add(JsonPrimitive(it.first)) } })
        put("additionalProperties", true)
    })
}

private fun JsonObject.string(name: String): String =
    get(name)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.int(name: String): Int? = get(name)?.jsonPrimitive?.intOrNull

private fun JsonObject.boolean(name: String): Boolean =
    get(name)?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false

private fun String.shellQuote(): String = "'" + replace("'", "'\\''") + "'"

private fun List<ByteArray>.flattenBytes(): ByteArray {
    val size = sumOf(ByteArray::size).coerceAtMost(1_048_576)
    val output = ByteArray(size)
    var offset = 0
    for (chunk in this) {
        if (offset >= size) break
        val count = minOf(chunk.size, size - offset)
        chunk.copyInto(output, offset, 0, count)
        offset += count
    }
    return output
}

private fun ByteArray.decodeToStringLimited(): String = decodeToString()
