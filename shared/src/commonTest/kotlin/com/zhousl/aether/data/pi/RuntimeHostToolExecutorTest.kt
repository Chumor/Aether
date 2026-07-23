package com.zhousl.aether.data.pi

import com.zhousl.aether.runtime.MultiplatformLocalRuntime
import com.zhousl.aether.runtime.RuntimeFileSystem
import com.zhousl.aether.runtime.RuntimeProcess
import com.zhousl.aether.runtime.RuntimeProcessExit
import com.zhousl.aether.runtime.RuntimeProcessSignal
import com.zhousl.aether.runtime.RuntimeProcessSpec
import com.zhousl.aether.runtime.RuntimeSetupProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class RuntimeHostToolExecutorTest {
    @Test
    fun readsRelativePathWithOffsetAndLineNumbers() = runTest {
        val runtime = HostToolFakeRuntime()
        runtime.files["/workspace/docs/readme.txt"] = "zero\none\ntwo\nthree".encodeToByteArray()

        val result = RuntimeHostToolExecutor(runtime).execute(
            "read",
            args {
                put("path", "docs/../docs/readme.txt")
                put("offset", 1)
                put("limit", 2)
                put("showLineNumbers", true)
            },
        ).json()

        assertEquals("2: one\n3: two", result.string("stdout"))
        assertEquals("/workspace/docs/readme.txt", result.string("path"))
        assertTrue(result["truncated"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun writeCreatesParentAndExpandsHomePath() = runTest {
        val runtime = HostToolFakeRuntime()

        val result = RuntimeHostToolExecutor(runtime).execute(
            "write",
            args {
                put("path", "~/notes/today.txt")
                put("content", "hello")
            },
        ).json()

        assertEquals("hello", runtime.files.getValue("/root/notes/today.txt").decodeToString())
        assertTrue("/root/notes" in runtime.directories)
        assertEquals("/root/notes/today.txt", result.string("path"))
    }

    @Test
    fun editRequiresExactlyOneMatch() = runTest {
        val runtime = HostToolFakeRuntime()
        runtime.files["/workspace/file.txt"] = "old and old".encodeToByteArray()

        val result = RuntimeHostToolExecutor(runtime).execute(
            "edit",
            args {
                put("path", "file.txt")
                put("oldText", "old")
                put("newText", "new")
            },
        )

        assertTrue(result.isError)
        assertTrue(result.json().string("error").contains("exactly once"))
        assertEquals("old and old", runtime.files.getValue("/workspace/file.txt").decodeToString())
    }

    @Test
    fun bashReturnsSeparateOutputAndExitStatus() = runTest {
        val runtime = HostToolFakeRuntime().apply {
            nextProcess = HostToolFakeProcess(
                stdoutChunks = listOf("hello ", "world\n"),
                stderrChunks = listOf("warning\n"),
                exitCode = 7,
            )
        }

        val result = RuntimeHostToolExecutor(runtime).execute(
            "bash",
            args {
                put("command", "printf test")
                put("working_directory", "/workspace/project/../project")
            },
        )
        val json = result.json()

        assertTrue(result.isError)
        assertEquals("hello world\n", json.string("stdout"))
        assertEquals("warning\n", json.string("stderr"))
        assertEquals("7", json["exit_code"]!!.jsonPrimitive.content)
        assertEquals("/workspace/project", runtime.lastSpec!!.workingDirectory)
        assertEquals(listOf("-lc", "printf test"), runtime.lastSpec!!.arguments)
        assertFalse(runtime.nextProcess.stdinOpen)
    }

    private fun args(block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): JsonObject =
        buildJsonObject(block)
}

private class HostToolFakeRuntime : MultiplatformLocalRuntime {
    override val homeDirectory = "/root"
    override val workspaceRoot = "/workspace"
    val files = mutableMapOf<String, ByteArray>()
    val directories = mutableSetOf<String>()
    var nextProcess = HostToolFakeProcess()
    var lastSpec: RuntimeProcessSpec? = null

    override val fileSystem: RuntimeFileSystem = object : RuntimeFileSystem {
        override suspend fun exists(path: String) = path in files || path in directories
        override suspend fun createDirectories(path: String) { directories += path }
        override suspend fun read(path: String) = files.getValue(path)
        override suspend fun write(path: String, content: ByteArray, executable: Boolean) {
            files[path] = content
        }
        override suspend fun remove(path: String, recursive: Boolean) { files.remove(path) }
        override suspend fun bindHostDirectory(hostPath: String, guestPath: String, readOnly: Boolean) = Unit
    }

    override suspend fun initialize(onProgress: (RuntimeSetupProgress) -> Unit) = Unit

    override suspend fun startProcess(spec: RuntimeProcessSpec): RuntimeProcess {
        lastSpec = spec
        return nextProcess
    }
}

private class HostToolFakeProcess(
    stdoutChunks: List<String> = emptyList(),
    stderrChunks: List<String> = emptyList(),
    private val exitCode: Int = 0,
) : RuntimeProcess {
    override val pid = 7
    override val stdout: Flow<ByteArray> = flowOf(*stdoutChunks.map(String::encodeToByteArray).toTypedArray())
    override val stderr: Flow<ByteArray> = flowOf(*stderrChunks.map(String::encodeToByteArray).toTypedArray())
    var stdinOpen = true

    override suspend fun writeStdin(bytes: ByteArray) = Unit
    override suspend fun closeStdin() { stdinOpen = false }
    override suspend fun awaitExit() = RuntimeProcessExit(exitCode)
    override suspend fun signal(signal: RuntimeProcessSignal) = Unit
}

private fun SharedHostToolResult.json(): JsonObject =
    Json.parseToJsonElement(outputJson).jsonObject

private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content
