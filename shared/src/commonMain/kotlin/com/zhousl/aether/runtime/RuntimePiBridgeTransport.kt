package com.zhousl.aether.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Starts the same long-lived JavaScript bridge on every local runtime. */
class RuntimePiBridgeTransport(
    private val runtime: MultiplatformLocalRuntime,
    private val nodeExecutable: String = "/usr/bin/node",
    private val bridgePath: String = "/root/.aether/pi-bridge/bridge.mjs",
    private val shutdownTimeoutMillis: Long = 2_000,
) : PiBridgeTransport {
    private val mutex = Mutex()
    private var activeProcess: RuntimeProcess? = null

    override suspend fun start(): RuntimeProcess = mutex.withLock {
        activeProcess?.let { return@withLock it }

        runtime.initialize()
        check(runtime.fileSystem.exists(bridgePath)) {
            "Pi Bridge is not installed at $bridgePath."
        }
        runtime.startProcess(
            RuntimeProcessSpec(
                executable = nodeExecutable,
                arguments = listOf(bridgePath),
                environment = mapOf(
                    "HOME" to runtime.homeDirectory,
                    "AETHER_WORKSPACE" to runtime.workspaceRoot,
                ),
                workingDirectory = bridgePath.substringBeforeLast('/'),
            ),
        ).also { activeProcess = it }
    }

    override suspend fun stop() {
        val process = mutex.withLock {
            activeProcess.also { activeProcess = null }
        } ?: return

        process.closeStdin()
        process.signal(RuntimeProcessSignal.Terminate)
        val exited = withTimeoutOrNull(shutdownTimeoutMillis) {
            process.awaitExit()
        }
        if (exited == null) {
            process.signal(RuntimeProcessSignal.Kill)
            process.awaitExit()
        }
    }
}
