package com.zhousl.aether.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhousl.aether.runtime.MultiplatformLocalRuntime
import com.zhousl.aether.runtime.RuntimeProcess
import com.zhousl.aether.runtime.RuntimeProcessSignal
import com.zhousl.aether.runtime.RuntimeProcessSpec
import com.zhousl.aether.platform.PlatformTerminalSurface
import com.zhousl.aether.platform.platformNativeTerminalAvailable
import com.zhousl.aether.shared.resources.Res
import com.zhousl.aether.shared.resources.back_label
import com.zhousl.aether.shared.resources.common_send
import com.zhousl.aether.shared.resources.settings_open_terminal
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val TerminalBackground = Color(0xFF111214)
private val TerminalSurface = Color(0xFF202226)
private val TerminalText = Color(0xFFE8E8E8)
private val TerminalMuted = Color(0xFF9EA2A8)

@Composable
fun SharedTerminalScreen(
    runtime: MultiplatformLocalRuntime,
    onBack: () -> Unit,
) {
    if (platformNativeTerminalAvailable) {
        var interruptSignal by remember { mutableStateOf(0) }
        Box(modifier = Modifier.fillMaxSize().background(TerminalBackground)) {
            PlatformTerminalSurface(
                runtime = runtime,
                interruptSignal = interruptSignal,
                modifier = Modifier.fillMaxSize().padding(top = 86.dp).navigationBarsPadding(),
            )
            TerminalTopBar(onBack = onBack, onInterrupt = { interruptSignal += 1 })
        }
        return
    }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var process by remember { mutableStateOf<RuntimeProcess?>(null) }
    var output by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Starting Alpine shell...") }

    fun appendOutput(value: String) {
        if (value.isEmpty()) return
        output = (output + value).takeLast(300_000)
    }

    fun submitInput(value: String = input) {
        val active = process ?: return
        if (value.isEmpty()) return
        appendOutput(value + "\n")
        input = ""
        scope.launch { active.writeStdin((value + "\n").encodeToByteArray()) }
    }

    LaunchedEffect(runtime) {
        runCatching {
            runtime.initialize()
            runtime.startProcess(
                RuntimeProcessSpec(
                    executable = "/bin/sh",
                    arguments = listOf("-l"),
                    environment = mapOf(
                        "HOME" to runtime.homeDirectory,
                        "TERM" to "xterm-256color",
                        "AETHER_WORKSPACE" to runtime.workspaceRoot,
                    ),
                    workingDirectory = runtime.workspaceRoot,
                )
            )
        }.fold(
            onSuccess = { active ->
                process = active
                status = "Alpine"
                launch { active.stdout.collect { appendOutput(it.decodeToString()) } }
                launch { active.stderr.collect { appendOutput(it.decodeToString()) } }
                launch {
                    val exit = active.awaitExit()
                    status = "Exited (${exit.exitCode})"
                    process = null
                }
            },
            onFailure = { status = it.message ?: "Unable to start Alpine shell." },
        )
    }
    LaunchedEffect(output) { scrollState.scrollTo(scrollState.maxValue) }
    DisposableEffect(process) {
        val active = process
        onDispose {
            if (active != null) scope.launch { active.signal(RuntimeProcessSignal.Terminate) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(TerminalBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurface)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    stringResource(Res.string.back_label),
                    tint = TerminalText,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    stringResource(Res.string.settings_open_terminal),
                    color = TerminalText,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(status, color = TerminalMuted, style = MaterialTheme.typography.labelSmall)
            }
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).clickable {
                    process?.let { active -> scope.launch { active.signal(RuntimeProcessSignal.Interrupt) } }
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, "Ctrl-C", tint = TerminalText, modifier = Modifier.size(19.dp))
            }
        }
        Text(
            text = output,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(14.dp),
            color = TerminalText,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurface)
                .imePadding()
                .navigationBarsPadding()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalBackground)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$ ", color = Color(0xFF67D391), fontFamily = FontFamily.Monospace)
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = TerminalText,
                        fontFamily = FontFamily.Monospace,
                    ),
                    cursorBrush = SolidColor(TerminalText),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submitInput() }),
                    singleLine = true,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF925BFF))
                    .clickable(enabled = input.isNotBlank()) { submitInput() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    stringResource(Res.string.common_send),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun TerminalTopBar(
    onBack: () -> Unit,
    onInterrupt: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalSurface)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                stringResource(Res.string.back_label),
                tint = TerminalText,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                stringResource(Res.string.settings_open_terminal),
                color = TerminalText,
                style = MaterialTheme.typography.titleSmall,
            )
            Text("Alpine", color = TerminalMuted, style = MaterialTheme.typography.labelSmall)
        }
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onInterrupt),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, "Ctrl-C", tint = TerminalText, modifier = Modifier.size(19.dp))
        }
    }
}
