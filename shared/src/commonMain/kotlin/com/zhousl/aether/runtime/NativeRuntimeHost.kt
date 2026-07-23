package com.zhousl.aether.runtime

/**
 * Callback-only ABI implemented by the iOS host. Keeping coroutines out of this
 * boundary makes the generated Objective-C/Swift API stable and easy to test.
 */
interface NativeRuntimeHost {
    fun initialize(listener: NativeRuntimeInitializationListener)
    fun startProcess(
        executable: String,
        arguments: List<String>,
        environment: Map<String, String>,
        workingDirectory: String,
        redirectErrorStream: Boolean,
        interactiveTerminal: Boolean,
        remoteDebuggingPipe: Boolean,
        listener: NativeRuntimeProcessListener,
    ): Long
    fun writeStdin(processId: Long, bytes: ByteArray): Boolean
    fun closeStdin(processId: Long)
    fun signal(processId: Long, signal: Int)
    fun resizeTerminal(processId: Long, columns: Int, rows: Int)

    fun fileExists(path: String, listener: NativeBooleanResultListener)
    fun createDirectories(path: String, listener: NativeUnitResultListener)
    fun readFile(path: String, listener: NativeBytesResultListener)
    fun writeFile(path: String, bytes: ByteArray, executable: Boolean, listener: NativeUnitResultListener)
    fun remove(path: String, recursive: Boolean, listener: NativeUnitResultListener)
    fun bindHostDirectory(hostPath: String, guestPath: String, readOnly: Boolean, listener: NativeUnitResultListener)
    fun pickFile(imagesOnly: Boolean, listener: NativePickedFileListener)
    fun copyText(text: String): Boolean
    fun shareText(title: String, text: String): Boolean
    fun openUrl(url: String): Boolean
}

interface NativeRuntimeInitializationListener {
    fun onProgress(phase: String, detail: String, fraction: Double)
    fun onOutput(text: String)
    fun onReady()
    fun onError(message: String)
}

interface NativeRuntimeProcessListener {
    fun onStdout(bytes: ByteArray)
    fun onStderr(bytes: ByteArray)
    fun onExit(exitCode: Int, signal: Int)
}

interface NativeBooleanResultListener {
    fun onSuccess(value: Boolean)
    fun onError(message: String)
}

interface NativeBytesResultListener {
    fun onSuccess(value: ByteArray)
    fun onError(message: String)
}

interface NativeUnitResultListener {
    fun onSuccess()
    fun onError(message: String)
}

interface NativePickedFileListener {
    fun onSelected(name: String, mimeType: String, bytes: ByteArray)
    fun onCancelled()
    fun onError(message: String)
}
