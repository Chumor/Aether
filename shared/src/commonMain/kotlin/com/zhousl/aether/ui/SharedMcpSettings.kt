package com.zhousl.aether.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.zhousl.aether.data.pi.SharedMcpManager
import com.zhousl.aether.data.pi.SharedMcpServerConfig
import com.zhousl.aether.data.pi.SharedMcpTransport
import com.zhousl.aether.shared.resources.Res
import com.zhousl.aether.shared.resources.mcp_title
import com.zhousl.aether.shared.resources.settings_no_mcp_servers
import com.zhousl.aether.ui.theme.AetherBackground
import com.zhousl.aether.ui.theme.AetherOnSurface
import com.zhousl.aether.ui.theme.AetherOnSurfaceVariant
import com.zhousl.aether.ui.theme.AetherSurface
import com.zhousl.aether.ui.theme.AetherSurfaceHigher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SharedMcpSettingsDetail(
    manager: SharedMcpManager,
    servers: List<SharedMcpServerConfig>,
    onServersChanged: (List<SharedMcpServerConfig>) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var transport by remember { mutableStateOf(SharedMcpTransport.Http) }
    var name by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    fun persist(updated: List<SharedMcpServerConfig>) {
        if (busy) return
        busy = true
        status = ""
        scope.launch {
            runCatching {
                manager.saveServers(updated)
                val tools = manager.refreshBindings(updated)
                onServersChanged(updated)
                status = "${tools.size} MCP tools available."
            }.onFailure { status = it.message.orEmpty() }
            busy = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AetherBackground)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(top = 98.dp, start = 20.dp, end = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsCardGroup {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(CircleShape).background(AetherSurface).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        McpModeChip("HTTP", transport == SharedMcpTransport.Http, Modifier.weight(1f)) { transport = SharedMcpTransport.Http }
                        McpModeChip("stdio", transport == SharedMcpTransport.Stdio, Modifier.weight(1f)) { transport = SharedMcpTransport.Stdio }
                    }
                    Spacer(Modifier.height(12.dp))
                    McpTextField(name, { name = it }, "Server name")
                    Spacer(Modifier.height(10.dp))
                    McpTextField(
                        endpoint,
                        { endpoint = it },
                        if (transport == SharedMcpTransport.Http) "https://server.example/mcp" else "command arg1 arg2",
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val parts = endpoint.trim().split(Regex("\\s+")).filter(String::isNotBlank)
                            val server = SharedMcpServerConfig(
                                name = name.trim(),
                                transport = transport,
                                url = if (transport == SharedMcpTransport.Http) endpoint.trim() else "",
                                command = if (transport == SharedMcpTransport.Stdio) parts.firstOrNull().orEmpty() else "",
                                arguments = if (transport == SharedMcpTransport.Stdio) parts.drop(1) else emptyList(),
                            )
                            persist(servers + server)
                            name = ""
                            endpoint = ""
                        },
                        enabled = name.isNotBlank() && endpoint.isNotBlank() && !busy,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AetherOnSurface, contentColor = AetherBackground),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Text("Add server")
                    }
                }
            }
            if (busy) CircularProgressIndicator(strokeWidth = 2.dp)
            if (status.isNotBlank()) {
                Text(status, color = if (status.contains("available")) AetherOnSurfaceVariant else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (servers.isEmpty() && !busy) Text(stringResource(Res.string.settings_no_mcp_servers), color = AetherOnSurfaceVariant)
            servers.forEach { server ->
                SettingsCardGroup {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(server.name, color = AetherOnSurface, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (server.transport == SharedMcpTransport.Http) server.url else listOf(server.command, *server.arguments.toTypedArray()).joinToString(" "),
                                color = AetherOnSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                            )
                        }
                        Switch(
                            checked = server.enabled,
                            onCheckedChange = { enabled -> persist(servers.map { if (it.id == server.id) it.copy(enabled = enabled) else it }) },
                        )
                        Box(
                            modifier = Modifier.padding(start = 4.dp).clip(CircleShape).clickable { persist(servers.filterNot { it.id == server.id }) }.padding(9.dp),
                        ) {
                            Icon(Icons.Rounded.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        SettingsTopBar(stringResource(Res.string.mcp_title), onBack)
    }
}

@Composable
private fun McpModeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(CircleShape).background(if (selected) AetherOnSurface else AetherSurfaceHigher)
            .clickable(onClick = onClick).padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) AetherBackground else AetherOnSurface, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun McpTextField(value: String, onValueChange: (String) -> Unit, hint: String) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AetherSurface).padding(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AetherOnSurface),
        cursorBrush = SolidColor(AetherOnSurface),
        singleLine = true,
        decorationBox = { field -> Box { if (value.isBlank()) Text(hint, color = AetherOnSurfaceVariant); field() } },
    )
}
