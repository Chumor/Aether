package com.zhousl.aether.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zhousl.aether.data.AetherAppStoreFallbackUrl
import com.zhousl.aether.data.AetherPrivacyPolicyUrl
import com.zhousl.aether.data.AetherWebsiteUrl
import com.zhousl.aether.data.AppSettings
import com.zhousl.aether.data.SharedAppUpdateService
import com.zhousl.aether.data.SharedAppUpdateStatus
import com.zhousl.aether.data.normalizeLlmInactivityReconnectTimeoutSeconds
import com.zhousl.aether.data.normalizeOldCommandHistoryRetentionHours
import com.zhousl.aether.data.normalizeTavilyBaseUrl
import com.zhousl.aether.data.parseAppSettings
import com.zhousl.aether.data.serializeAppSettings
import com.zhousl.aether.data.pi.SharedPiUsage
import com.zhousl.aether.platform.PlatformCapabilities
import com.zhousl.aether.platform.PlatformServices
import com.zhousl.aether.platform.platformAppVersion
import com.zhousl.aether.shared.resources.Res
import com.zhousl.aether.shared.resources.*
import com.zhousl.aether.ui.theme.AetherBackground
import com.zhousl.aether.ui.theme.AetherOnSurface
import com.zhousl.aether.ui.theme.AetherOnSurfaceVariant
import com.zhousl.aether.ui.theme.AetherPrimary
import com.zhousl.aether.ui.theme.AetherSurface
import com.zhousl.aether.ui.theme.AetherSurfaceHigher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SharedPersonalizationSettingsDetail(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    var systemPrompt by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    SharedSettingsDetailScaffold(
        title = stringResource(Res.string.personalization_title),
        onBack = onBack,
    ) {
        SettingsCardGroup {
            SharedSettingsTextField(
                label = stringResource(Res.string.settings_custom_instructions),
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                minLines = 8,
            )
        }
        Text(
            text = stringResource(Res.string.settings_custom_instructions_variables_hint),
            style = MaterialTheme.typography.bodySmall,
            color = AetherOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        SharedSettingsSaveButton {
            onSave(settings.copy(systemPrompt = systemPrompt.trim()))
            onBack()
        }
    }
}

@Composable
internal fun SharedWebToolsSettingsDetail(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    var apiKey by remember(settings.tavilyApiKey) { mutableStateOf(settings.tavilyApiKey) }
    var baseUrl by remember(settings.tavilyBaseUrl) { mutableStateOf(settings.tavilyBaseUrl) }
    SharedSettingsDetailScaffold(
        title = stringResource(Res.string.web_tools_title),
        onBack = onBack,
    ) {
        SettingsCardGroup {
            SharedSettingsTextField(
                label = stringResource(Res.string.settings_tavily_api_key),
                value = apiKey,
                onValueChange = { apiKey = it },
                keyboardType = KeyboardType.Password,
                secret = true,
            )
            CardDivider()
            SharedSettingsTextField(
                label = stringResource(Res.string.settings_tavily_base_url),
                value = baseUrl,
                onValueChange = { baseUrl = it },
                keyboardType = KeyboardType.Uri,
            )
        }
        Text(
            text = stringResource(Res.string.settings_web_tools_description),
            style = MaterialTheme.typography.bodySmall,
            color = AetherOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        SharedSettingsSaveButton {
            onSave(
                settings.copy(
                    tavilyApiKey = apiKey.trim(),
                    tavilyBaseUrl = normalizeTavilyBaseUrl(baseUrl),
                )
            )
            onBack()
        }
    }
}

@Composable
internal fun SharedReliabilitySettingsDetail(
    settings: AppSettings,
    capabilities: PlatformCapabilities,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    var reconnectSeconds by remember(settings.llmInactivityReconnectTimeoutSeconds) {
        mutableStateOf(settings.llmInactivityReconnectTimeoutSeconds.toString())
    }
    var keepBackground by remember(settings.keepTasksRunningInBackground) {
        mutableStateOf(settings.keepTasksRunningInBackground)
    }
    var notify by remember(settings.notifyOnTaskCompletion) {
        mutableStateOf(settings.notifyOnTaskCompletion)
    }
    SharedSettingsDetailScaffold(
        title = stringResource(Res.string.reliability_title),
        onBack = onBack,
    ) {
        Text(
            stringResource(Res.string.settings_multitasking),
            style = MaterialTheme.typography.labelLarge,
            color = AetherOnSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        SettingsCardGroup {
            if (capabilities.persistentBackground) {
                SharedSettingsToggle(
                    title = stringResource(Res.string.settings_keep_tasks_running_background),
                    subtitle = stringResource(Res.string.settings_keep_tasks_running_background_subtitle),
                    checked = keepBackground,
                    onCheckedChange = { keepBackground = it },
                )
                CardDivider()
                SharedSettingsToggle(
                    title = stringResource(Res.string.settings_notify_background_tasks_finish),
                    subtitle = stringResource(Res.string.settings_notify_background_tasks_finish_subtitle),
                    checked = notify,
                    onCheckedChange = { notify = it },
                )
            } else {
                Text(
                    text = stringResource(Res.string.settings_ios_background_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherOnSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Text(
            stringResource(Res.string.settings_reconnect),
            style = MaterialTheme.typography.labelLarge,
            color = AetherOnSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        SettingsCardGroup {
            SharedSettingsTextField(
                label = stringResource(Res.string.settings_reconnect_after_idle_seconds),
                value = reconnectSeconds,
                onValueChange = { reconnectSeconds = it.filter(Char::isDigit) },
                keyboardType = KeyboardType.Number,
            )
        }
        Text(
            text = stringResource(Res.string.settings_reconnect_after_idle_description),
            style = MaterialTheme.typography.bodySmall,
            color = AetherOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        SharedSettingsSaveButton {
            onSave(
                settings.copy(
                    llmInactivityReconnectTimeoutSeconds =
                        normalizeLlmInactivityReconnectTimeoutSeconds(reconnectSeconds.toIntOrNull()),
                    keepTasksRunningInBackground =
                        if (capabilities.persistentBackground) keepBackground else false,
                    notifyOnTaskCompletion =
                        if (capabilities.persistentBackground) notify else false,
                )
            )
            onBack()
        }
    }
}

@Composable
internal fun SharedStatisticsSettingsDetail(
    usage: List<SharedPiUsage>,
    onBack: () -> Unit,
) {
    val input = usage.sumOf { it.inputTokens }
    val output = usage.sumOf { it.outputTokens }
    val reasoning = usage.sumOf { it.reasoningTokens }
    val total = usage.sumOf { it.totalTokens }
    SharedSettingsDetailScaffold(
        title = stringResource(Res.string.statistics_title),
        onBack = onBack,
    ) {
        SettingsCardGroup {
            Text(
                stringResource(Res.string.statistics_overview),
                style = MaterialTheme.typography.titleMedium,
                color = AetherOnSurface,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SharedMetricTile(
                    stringResource(Res.string.statistics_total_tokens),
                    formatTokenCount(total),
                    Modifier.weight(1f),
                )
                SharedMetricTile(
                    stringResource(Res.string.statistics_recorded_turns),
                    usage.size.toString(),
                    Modifier.weight(1f),
                )
            }
        }
        SettingsCardGroup {
            SharedValueRow(stringResource(Res.string.statistics_input), formatTokenCount(input))
            CardDivider()
            SharedValueRow(stringResource(Res.string.statistics_output), formatTokenCount(output))
            CardDivider()
            SharedValueRow(stringResource(Res.string.statistics_reasoning), formatTokenCount(reasoning))
        }
        if (usage.isEmpty()) {
            Text(
                stringResource(Res.string.settings_statistics_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }
    }
}

@Composable
internal fun SharedDeveloperSettingsDetail(
    settings: AppSettings,
    platformServices: PlatformServices,
    onSave: (AppSettings) -> Unit,
    onReplayOnboarding: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var autoClean by remember(settings.autoCleanOldCommandHistory) {
        mutableStateOf(settings.autoCleanOldCommandHistory)
    }
    var retention by remember(settings.oldCommandHistoryRetentionHours) {
        mutableStateOf(settings.oldCommandHistoryRetentionHours.toString())
    }
    var status by remember { mutableStateOf("") }
    SharedSettingsDetailScaffold(
        title = stringResource(Res.string.developer_title),
        onBack = onBack,
    ) {
        Text(
            text = stringResource(Res.string.settings_developer_description),
            style = MaterialTheme.typography.bodySmall,
            color = AetherOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        SettingsCardGroup {
            SharedSettingsAction(
                stringResource(Res.string.settings_export_app_data),
                stringResource(Res.string.settings_app_data_description),
            ) {
                platformServices.shareText("Aether data", serializeAppSettings(settings))
            }
            CardDivider()
            SharedSettingsAction(
                stringResource(Res.string.settings_import_app_data),
                stringResource(Res.string.settings_app_data_description),
            ) {
                scope.launch {
                    val file = platformServices.pickFile()
                    if (file != null) {
                        runCatching {
                            parseAppSettings(file.bytes.decodeToString(), settings)
                        }.onSuccess {
                            onSave(it)
                            autoClean = it.autoCleanOldCommandHistory
                            retention = it.oldCommandHistoryRetentionHours.toString()
                            status = "Imported " + file.name
                        }.onFailure {
                            status = it.message.orEmpty()
                        }
                    }
                }
            }
        }
        SettingsCardGroup {
            SharedSettingsToggle(
                title = stringResource(Res.string.settings_old_command_history_retention_hours),
                subtitle = stringResource(Res.string.settings_old_command_history_retention_hours_description),
                checked = autoClean,
                onCheckedChange = { autoClean = it },
            )
            if (autoClean) {
                CardDivider()
                SharedSettingsTextField(
                    label = stringResource(Res.string.settings_old_command_history_retention_hours_value),
                    value = retention,
                    onValueChange = { retention = it.filter(Char::isDigit) },
                    keyboardType = KeyboardType.Number,
                )
            }
        }
        SettingsCardGroup {
            SharedSettingsAction(
                stringResource(Res.string.settings_replay_follow_up_tour),
                stringResource(Res.string.settings_replay_follow_up_tour_description),
                onReplayOnboarding,
            )
        }
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodySmall, color = AetherOnSurfaceVariant)
        }
        SharedSettingsSaveButton {
            onSave(
                settings.copy(
                    autoCleanOldCommandHistory = autoClean,
                    oldCommandHistoryRetentionHours =
                        normalizeOldCommandHistoryRetentionHours(retention.toIntOrNull()),
                )
            )
            onBack()
        }
    }
}

@Composable
internal fun SharedAboutSettingsDetail(
    platformServices: PlatformServices,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val installedVersion = remember { platformAppVersion() }
    val service = remember { SharedAppUpdateService() }
    var checking by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<SharedAppUpdateStatus?>(null) }
    var checkFailed by remember { mutableStateOf(false) }
    val updateSubtitle = when {
        checking -> stringResource(Res.string.settings_app_store_checking)
        checkFailed -> stringResource(Res.string.settings_update_check_failed_short)
        update?.isUpdateAvailable == true ->
            stringResource(Res.string.settings_app_store_update_available, update?.storeVersion.orEmpty())
        update?.isPublished == true -> stringResource(Res.string.settings_app_store_current)
        update != null -> stringResource(Res.string.settings_app_store_unpublished)
        else -> stringResource(Res.string.settings_check_for_updates)
    }
    SharedSettingsDetailScaffold(
        title = stringResource(Res.string.about_title),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.aether_mark),
                contentDescription = stringResource(Res.string.settings_aether_logo),
                modifier = Modifier.size(104.dp),
            )
            Text("Aether", style = MaterialTheme.typography.titleLarge, color = AetherOnSurface)
            Text(
                stringResource(Res.string.settings_release_summary, installedVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherOnSurfaceVariant,
            )
        }
        SettingsCardGroup {
            SharedValueRow(stringResource(Res.string.settings_author), "Zhou-Shilin")
            CardDivider()
            SharedValueRow(stringResource(Res.string.settings_version), installedVersion)
            CardDivider()
            SettingsNavRow(
                icon = Icons.Rounded.CloudDownload,
                title = stringResource(Res.string.settings_check_for_updates),
                subtitle = updateSubtitle,
                showChevron = false,
                enabled = !checking,
            ) {
                checking = true
                checkFailed = false
                scope.launch {
                    runCatching { service.check(installedVersion) }
                        .onSuccess { update = it }
                        .onFailure { checkFailed = true }
                    checking = false
                }
            }
            CardDivider()
            SettingsNavRow(
                icon = Icons.Rounded.Link,
                title = stringResource(Res.string.settings_website),
                subtitle = AetherWebsiteUrl.removePrefix("https://"),
            ) { platformServices.openUrl(AetherWebsiteUrl) }
            CardDivider()
            SettingsNavRow(
                icon = Icons.Rounded.PrivacyTip,
                title = stringResource(Res.string.settings_privacy_policy),
                subtitle = AetherPrivacyPolicyUrl.removePrefix("https://"),
            ) { platformServices.openUrl(AetherPrivacyPolicyUrl) }
        }
        if (update?.isUpdateAvailable == true || update?.isPublished == true) {
            SharedSettingsSaveButton(
                label = stringResource(Res.string.settings_open_app_store),
            ) {
                platformServices.openUrl(update?.storeUrl.orEmpty().ifBlank { AetherAppStoreFallbackUrl })
            }
        }
    }
}

@Composable
private fun SharedSettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(AetherBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 98.dp, start = 20.dp, end = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
        SettingsTopBar(title = title, onBack = onBack)
    }
}

@Composable
private fun SharedSettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    minLines: Int = 1,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AetherOnSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AetherSurface)
                .padding(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AetherOnSurface),
            cursorBrush = SolidColor(AetherPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            minLines = minLines,
            maxLines = maxOf(minLines, 12),
        )
    }
}

@Composable
private fun SharedSettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = AetherOnSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AetherOnSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AetherPrimary,
                checkedThumbColor = AetherBackground,
            ),
        )
    }
}

@Composable
private fun SharedSettingsAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = AetherOnSurface)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AetherOnSurfaceVariant)
    }
}

@Composable
private fun SharedSettingsSaveButton(
    label: String = stringResource(Res.string.common_save),
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = AetherOnSurface,
            contentColor = AetherBackground,
        ),
    ) {
        Text(label)
    }
}

@Composable
private fun SharedMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(AetherSurfaceHigher).padding(14.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = AetherOnSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AetherOnSurfaceVariant)
    }
}

@Composable
private fun SharedValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AetherOnSurfaceVariant)
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = AetherOnSurface,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatTokenCount(tokens: Long): String = when {
    tokens >= 1_000_000 -> ((tokens / 100_000) / 10.0).toString() + "M"
    tokens >= 1_000 -> ((tokens / 100) / 10.0).toString() + "K"
    else -> tokens.toString()
}
