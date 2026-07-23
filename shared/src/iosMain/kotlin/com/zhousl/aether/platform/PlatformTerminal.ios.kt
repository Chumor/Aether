package com.zhousl.aether.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.zhousl.aether.runtime.MultiplatformLocalRuntime
import com.zhousl.aether.runtime.RuntimeProcess
import com.zhousl.aether.runtime.RuntimeProcessSignal
import com.zhousl.aether.runtime.RuntimeProcessSpec
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.UIKit.UIApplication
import platform.darwin.NSObject

actual val platformNativeTerminalAvailable: Boolean = true

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformTerminalSurface(
    runtime: MultiplatformLocalRuntime,
    interruptSignal: Int,
    modifier: Modifier,
) {
    val bridge = remember(runtime) { IosHtermBridge() }
    LaunchedEffect(runtime, bridge) {
        runtime.initialize()
        val process = runtime.startProcess(
            RuntimeProcessSpec(
                executable = "/bin/sh",
                arguments = listOf("-l"),
                environment = mapOf("TERM" to "xterm-256color", "HOME" to runtime.homeDirectory),
                workingDirectory = runtime.homeDirectory,
                interactiveTerminal = true,
            )
        )
        bridge.attach(process)
        coroutineScope {
            launch { process.stdout.collect(bridge::write) }
            launch { process.stderr.collect(bridge::write) }
            process.awaitExit()
        }
    }
    DisposableEffect(bridge) {
        onDispose { bridge.close() }
    }
    LaunchedEffect(interruptSignal) {
        if (interruptSignal > 0) bridge.interrupt()
    }
    UIKitView(
        modifier = modifier,
        factory = { bridge.webView },
        update = { bridge.focus() },
    )
}

@OptIn(ExperimentalForeignApi::class)
private class IosHtermBridge : NSObject(), WKScriptMessageHandlerProtocol {
    private val scope = kotlinx.coroutines.MainScope()
    private var process: RuntimeProcess? = null
    private var loaded = false
    private val pending = mutableListOf<ByteArray>()
    private val configuration = WKWebViewConfiguration().apply {
        listOf("load", "sendInput", "resize", "propUpdate", "focus", "syncFocus", "newScrollHeight", "newScrollTop", "openLink")
            .forEach { userContentController.addScriptMessageHandler(this@IosHtermBridge, it) }
    }
    val webView = WKWebView(
        frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
        configuration = configuration,
    ).apply {
        opaque = false
        backgroundColor = platform.UIKit.UIColor.clearColor
        scrollView.backgroundColor = platform.UIKit.UIColor.clearColor
        val resource = NSBundle.mainBundle.URLForResource("term", "html")
        if (resource != null) loadFileURL(resource, allowingReadAccessToURL = resource.URLByDeletingLastPathComponent ?: resource)
    }

    fun attach(process: RuntimeProcess) {
        this.process = process
    }

    fun write(bytes: ByteArray) {
        if (!loaded) {
            pending += bytes.copyOf()
            return
        }
        val latin1 = buildString(bytes.size) { bytes.forEach { append((it.toInt() and 0xff).toChar()) } }
        webView.evaluateJavaScript("exports.write(${JsonPrimitive(latin1)})", null)
    }

    fun focus() {
        if (loaded) webView.evaluateJavaScript("exports.setFocused(true);term.scrollPort_.screen_.contentEditable=true;term.focus()", null)
    }

    suspend fun interrupt() {
        process?.signal(RuntimeProcessSignal.Interrupt)
    }

    fun close() {
        val running = process
        process = null
        if (running != null) {
            scope.launch { running.signal(RuntimeProcessSignal.Terminate) }
        }
        scope.cancel()
        configuration.userContentController.removeAllScriptMessageHandlers()
    }

    override fun userContentController(userContentController: WKUserContentController, didReceiveScriptMessage: WKScriptMessage) {
        when (didReceiveScriptMessage.name) {
            "load" -> {
                loaded = true
                webView.evaluateJavaScript(
                    "exports.updateStyle({foregroundColor:'#ececf1',backgroundColor:'#151517',fontFamily:'ui-monospace, Menlo, monospace',fontSize:14,colorPaletteOverrides:{},blinkCursor:true,cursorShape:'BLOCK'});term.scrollPort_.screen_.contentEditable=true;term.focus()",
                    null,
                )
                val buffered = pending.toList()
                pending.clear()
                buffered.forEach(::write)
            }
            "sendInput" -> {
                val text = didReceiveScriptMessage.body as? String ?: return
                val target = process ?: return
                scope.launch { target.writeStdin(text.encodeToByteArray()) }
            }
            "resize" -> {
                val target = process ?: return
                webView.evaluateJavaScript("JSON.stringify(exports.getSize())") { value, _ ->
                    val dimensions = (value as? String)
                        ?.removePrefix("[")
                        ?.removeSuffix("]")
                        ?.split(',')
                        ?.mapNotNull { it.trim().toIntOrNull() }
                        .orEmpty()
                    if (dimensions.size == 2) {
                        scope.launch { target.resize(dimensions[0], dimensions[1]) }
                    }
                }
            }
            "openLink" -> {
                val url = didReceiveScriptMessage.body as? String ?: return
                NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it) }
            }
        }
    }
}
