package com.zhousl.aether.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zhousl.aether.runtime.MultiplatformLocalRuntime

expect val platformNativeTerminalAvailable: Boolean

@Composable
expect fun PlatformTerminalSurface(
    runtime: MultiplatformLocalRuntime,
    interruptSignal: Int,
    modifier: Modifier = Modifier,
)
