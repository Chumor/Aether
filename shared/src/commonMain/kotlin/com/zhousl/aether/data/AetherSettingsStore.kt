package com.zhousl.aether.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

data class SharedPersistedSettings(
    val providerConfigs: List<LlmProviderConfig> = emptyList(),
    val activeProviderConfigId: String = "",
    val onboardingCompletedVersion: Int = 0,
    val appSettings: AppSettings = AppSettings(),
) {
    val activeProviderConfig: LlmProviderConfig?
        get() = providerConfigs.firstOrNull { it.id == activeProviderConfigId }
            ?: providerConfigs.firstOrNull()
}

class AetherSettingsStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun load(): SharedPersistedSettings {
        val preferences = dataStore.data.first()
        val defaults = AppSettings()
        val legacySettings = defaults.copy(
            language = AppLanguage.fromStorage(preferences[Language]),
            themeMode = AppThemeMode.fromStorage(preferences[ThemeMode]),
            systemPrompt = preferences[SystemPrompt] ?: defaults.systemPrompt,
            reasoningEffort = normalizeReasoningEffort(preferences[ReasoningEffort]),
            tavilyApiKey = preferences[TavilyApiKey].orEmpty(),
            tavilyBaseUrl = normalizeTavilyBaseUrl(
                preferences[TavilyBaseUrl] ?: defaults.tavilyBaseUrl,
            ),
            onboardingCompletedVersion = preferences[OnboardingCompletedVersion] ?: 0,
        )
        val fullSettings = parseAppSettings(preferences[AppSettingsJson].orEmpty(), legacySettings)
        return SharedPersistedSettings(
            providerConfigs = parseProviderConfigs(preferences[ProviderConfigs].orEmpty()),
            activeProviderConfigId = preferences[ActiveProviderConfigId].orEmpty(),
            onboardingCompletedVersion = preferences[OnboardingCompletedVersion] ?: 0,
            appSettings = fullSettings.copy(
                onboardingCompletedVersion = preferences[OnboardingCompletedVersion] ?: 0,
            ),
        )
    }

    suspend fun saveProvider(config: LlmProviderConfig) {
        dataStore.edit { preferences ->
            val current = parseProviderConfigs(preferences[ProviderConfigs].orEmpty())
            val updated = current.filterNot { it.id == config.id } + config
            preferences[ProviderConfigs] = serializeProviderConfigs(updated)
            preferences[ActiveProviderConfigId] = config.id
            preferences[OnboardingCompletedVersion] = CurrentOnboardingVersion
        }
    }

    suspend fun saveGeneralSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[AppSettingsJson] = serializeAppSettings(settings)
            preferences[Language] = settings.language.storageValue
            preferences[ThemeMode] = settings.themeMode.storageValue
            preferences[SystemPrompt] = settings.systemPrompt
            preferences[ReasoningEffort] = normalizeReasoningEffort(settings.reasoningEffort)
            preferences[TavilyApiKey] = settings.tavilyApiKey
            preferences[TavilyBaseUrl] = normalizeTavilyBaseUrl(settings.tavilyBaseUrl)
        }
    }

    suspend fun markOnboardingComplete() {
        dataStore.edit { preferences ->
            preferences[OnboardingCompletedVersion] = CurrentOnboardingVersion
            val current = parseAppSettings(preferences[AppSettingsJson].orEmpty())
            preferences[AppSettingsJson] = serializeAppSettings(
                current.copy(
                    onboardingSeenVersion = CurrentOnboardingVersion,
                    onboardingCompletedVersion = CurrentOnboardingVersion,
                )
            )
        }
    }

    private companion object {
        val ProviderConfigs = stringPreferencesKey("provider_configs")
        val ActiveProviderConfigId = stringPreferencesKey("provider_config_id")
        val OnboardingCompletedVersion = intPreferencesKey("onboarding_completed_version")
        val Language = stringPreferencesKey("language")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val SystemPrompt = stringPreferencesKey("system_prompt")
        val ReasoningEffort = stringPreferencesKey("reasoning_effort")
        val TavilyApiKey = stringPreferencesKey("tavily_api_key")
        val TavilyBaseUrl = stringPreferencesKey("tavily_base_url")
        val AppSettingsJson = stringPreferencesKey("app_settings_json")
    }
}

fun createAetherSettingsStore(path: String): AetherSettingsStore = AetherSettingsStore(
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { path.toPath() },
    )
)
