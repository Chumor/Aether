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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.zhousl.aether.data.AppLanguage
import com.zhousl.aether.data.AppSettings
import com.zhousl.aether.data.AppThemeMode
import com.zhousl.aether.shared.resources.Res
import com.zhousl.aether.shared.resources.common_save
import com.zhousl.aether.shared.resources.general_title
import com.zhousl.aether.shared.resources.language_english
import com.zhousl.aether.shared.resources.language_simplified_chinese
import com.zhousl.aether.shared.resources.settings_language
import com.zhousl.aether.shared.resources.settings_language_description
import com.zhousl.aether.shared.resources.settings_system_prompt
import com.zhousl.aether.shared.resources.settings_system_prompt_description
import com.zhousl.aether.shared.resources.settings_theme
import com.zhousl.aether.shared.resources.settings_theme_description
import com.zhousl.aether.shared.resources.theme_dark
import com.zhousl.aether.shared.resources.theme_light
import com.zhousl.aether.shared.resources.theme_system
import com.zhousl.aether.ui.theme.AetherBackground
import com.zhousl.aether.ui.theme.AetherOnSurface
import com.zhousl.aether.ui.theme.AetherOnSurfaceVariant
import com.zhousl.aether.ui.theme.AetherSurface
import com.zhousl.aether.ui.theme.AetherSurfaceHigher
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SharedGeneralSettingsDetail(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    var language by remember(settings.language) { mutableStateOf(settings.language) }
    var themeMode by remember(settings.themeMode) { mutableStateOf(settings.themeMode) }
    var systemPrompt by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }

    Box(modifier = Modifier.fillMaxSize().background(AetherBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 98.dp, start = 20.dp, end = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCardGroup {
                SettingsSectionHeader(
                    title = stringResource(Res.string.settings_language),
                    description = stringResource(Res.string.settings_language_description),
                )
                SegmentedChoice(
                    entries = listOf(
                        AppLanguage.English to stringResource(Res.string.language_english),
                        AppLanguage.SimplifiedChinese to stringResource(Res.string.language_simplified_chinese),
                    ),
                    selected = language,
                    onSelected = { language = it },
                )
            }
            SettingsCardGroup {
                SettingsSectionHeader(
                    title = stringResource(Res.string.settings_theme),
                    description = stringResource(Res.string.settings_theme_description),
                )
                SegmentedChoice(
                    entries = listOf(
                        AppThemeMode.System to stringResource(Res.string.theme_system),
                        AppThemeMode.Light to stringResource(Res.string.theme_light),
                        AppThemeMode.Dark to stringResource(Res.string.theme_dark),
                    ),
                    selected = themeMode,
                    onSelected = { themeMode = it },
                )
            }
            SettingsCardGroup {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        stringResource(Res.string.settings_system_prompt),
                        style = MaterialTheme.typography.titleMedium,
                        color = AetherOnSurface,
                    )
                    Text(
                        stringResource(Res.string.settings_system_prompt_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = AetherOnSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    BasicTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AetherSurface)
                            .padding(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AetherOnSurface),
                        cursorBrush = SolidColor(AetherOnSurface),
                        minLines = 5,
                        maxLines = 12,
                    )
                }
            }
            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            language = language,
                            themeMode = themeMode,
                            systemPrompt = systemPrompt.trim(),
                        )
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AetherOnSurface,
                    contentColor = AetherBackground,
                ),
            ) {
                Text(stringResource(Res.string.common_save))
            }
            Spacer(Modifier.height(24.dp))
        }
        SettingsTopBar(title = stringResource(Res.string.general_title), onBack = onBack)
    }
}

@Composable
private fun SettingsSectionHeader(title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = AetherOnSurface)
        Text(description, style = MaterialTheme.typography.bodySmall, color = AetherOnSurfaceVariant)
    }
}

@Composable
private fun <T> SegmentedChoice(
    entries: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        entries.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (active) AetherOnSurface else AetherSurfaceHigher)
                    .clickable { onSelected(value) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (active) AetherBackground else AetherOnSurface,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
