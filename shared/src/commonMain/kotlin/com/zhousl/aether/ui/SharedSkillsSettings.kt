package com.zhousl.aether.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.zhousl.aether.data.SharedInstalledSkill
import com.zhousl.aether.data.SharedSkillManager
import com.zhousl.aether.data.platformRandomUuid
import com.zhousl.aether.platform.PlatformServices
import com.zhousl.aether.runtime.MultiplatformLocalRuntime
import com.zhousl.aether.shared.resources.Res
import com.zhousl.aether.shared.resources.common_install
import com.zhousl.aether.shared.resources.common_remove
import com.zhousl.aether.shared.resources.settings_add_skill
import com.zhousl.aether.shared.resources.settings_no_skills_installed
import com.zhousl.aether.shared.resources.settings_remote_skill_url
import com.zhousl.aether.shared.resources.skills_title
import com.zhousl.aether.ui.theme.AetherBackground
import com.zhousl.aether.ui.theme.AetherOnSurface
import com.zhousl.aether.ui.theme.AetherOnSurfaceVariant
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch

@Composable
internal fun SharedSkillsSettingsDetail(
    skillManager: SharedSkillManager,
    runtime: MultiplatformLocalRuntime,
    platformServices: PlatformServices,
    installedSkills: List<SharedInstalledSkill>,
    onSkillsChanged: (List<SharedInstalledSkill>) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var remoteUrl by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    fun runOperation(operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        status = ""
        scope.launch {
            runCatching { operation() }
                .onSuccess { onSkillsChanged(skillManager.list()) }
                .onFailure { status = it.message.orEmpty() }
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
                    Text(stringResource(Res.string.settings_add_skill), style = MaterialTheme.typography.titleMedium, color = AetherOnSurface)
                    Spacer(Modifier.height(12.dp))
                    BasicTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AetherOnSurface),
                        cursorBrush = SolidColor(AetherOnSurface),
                        singleLine = true,
                        decorationBox = { field ->
                            Box {
                                if (remoteUrl.isBlank()) Text(stringResource(Res.string.settings_remote_skill_url), color = AetherOnSurfaceVariant)
                                field()
                            }
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val url = remoteUrl.trim()
                                runOperation { skillManager.installRemote(url) }
                                remoteUrl = ""
                            },
                            enabled = remoteUrl.startsWith("https://") && !busy,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AetherOnSurface, contentColor = AetherBackground),
                        ) {
                            Icon(Icons.Rounded.Add, null)
                            Text(stringResource(Res.string.common_install))
                        }
                        Button(
                            onClick = {
                                runOperation {
                                    val picked = platformServices.pickFile(false) ?: return@runOperation
                                    val archive = "${runtime.workspaceRoot}/.skill-${platformRandomUuid()}.zip"
                                    runtime.fileSystem.write(archive, picked.bytes)
                                    try { skillManager.installArchive(archive) } finally { runtime.fileSystem.remove(archive) }
                                }
                            },
                            enabled = !busy,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Text("ZIP", color = AetherOnSurface)
                        }
                    }
                }
            }
            if (busy) CircularProgressIndicator(modifier = Modifier.padding(8.dp), strokeWidth = 2.dp)
            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (installedSkills.isEmpty() && !busy) {
                Text(stringResource(Res.string.settings_no_skills_installed), color = AetherOnSurfaceVariant)
            }
            installedSkills.forEach { skill ->
                SettingsCardGroup {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(skill.name, style = MaterialTheme.typography.titleMedium, color = AetherOnSurface)
                        if (skill.description.isNotBlank()) {
                            Text(skill.description, style = MaterialTheme.typography.bodySmall, color = AetherOnSurfaceVariant)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { runOperation { skillManager.remove(skill.id) } },
                            enabled = !busy,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Icon(Icons.Rounded.Close, null)
                            Text(stringResource(Res.string.common_remove), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        SettingsTopBar(stringResource(Res.string.skills_title), onBack)
    }
}
