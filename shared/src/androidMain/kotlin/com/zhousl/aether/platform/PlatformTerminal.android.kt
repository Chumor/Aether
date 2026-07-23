package com.zhousl.aether.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zhousl.aether.runtime.MultiplatformLocalRuntime

actual val platformNativeTerminalAvailable: Boolean = false

@Composable
actual fun PlatformTerminalSurface(
    runtime: MultiplatformLocalRuntime,
    interruptSignal: Int,
    modifier: Modifier,
) {
    Box(modifier)
}
