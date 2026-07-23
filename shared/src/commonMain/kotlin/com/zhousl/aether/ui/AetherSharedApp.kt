package com.zhousl.aether.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhousl.aether.platform.PlatformCapabilities
import com.zhousl.aether.platform.PlatformServices
import com.zhousl.aether.platform.NoOpPlatformServices
import com.zhousl.aether.platform.BackgroundExecutionLease
import com.zhousl.aether.platform.SharedApplicationLifecycle
import com.zhousl.aether.platform.createBackgroundExecutionManager
import com.zhousl.aether.platform.applyPlatformAppLanguage
import com.zhousl.aether.data.LlmProviderConfig
import com.zhousl.aether.data.AetherSettingsStore
import com.zhousl.aether.data.AppSettings
import com.zhousl.aether.data.CurrentOnboardingVersion
import com.zhousl.aether.data.SharedSkillManager
import com.zhousl.aether.data.SharedInstalledSkill
import com.zhousl.aether.data.SharedAetherExtensionManager
import com.zhousl.aether.data.SharedAetherExtensionSnapshot
import com.zhousl.aether.data.SharedProviderModelCatalogClient
import com.zhousl.aether.data.PiProviderCatalog
import com.zhousl.aether.data.ProviderAuthMethod
import com.zhousl.aether.data.platformRandomUuid
import com.zhousl.aether.data.pi.PiProviderAuthState
import com.zhousl.aether.data.pi.SharedPiChatClient
import com.zhousl.aether.data.pi.SharedPiChatMessage
import com.zhousl.aether.data.pi.SharedPiUsage
import com.zhousl.aether.data.pi.SharedPiImage
import com.zhousl.aether.data.pi.RuntimeHostToolExecutor
import com.zhousl.aether.data.pi.SharedMcpManager
import com.zhousl.aether.data.pi.SharedMcpServerConfig
import com.zhousl.aether.data.pi.SharedToolRegistry
import com.zhousl.aether.data.pi.SharedChromeManager
import com.zhousl.aether.data.pi.SharedCompositeHostTools
import com.zhousl.aether.data.pi.SharedWebToolExecutor
import com.zhousl.aether.data.pi.toPiOAuthPrompt
import com.zhousl.aether.data.pi.toPiProviderEnvironmentVariables
import com.zhousl.aether.data.chatdb.ChatHistoryDatabase
import com.zhousl.aether.data.chatdb.PersistedChatMessage
import com.zhousl.aether.data.chatdb.PersistedChatTool
import com.zhousl.aether.data.chatdb.PersistedChatAttachment
import com.zhousl.aether.data.chatdb.PersistedChatUsage
import com.zhousl.aether.data.chatdb.SharedChatHistoryStore
import com.zhousl.aether.runtime.MultiplatformLocalRuntime
import com.zhousl.aether.runtime.RuntimePiBridgeTransport
import com.zhousl.aether.runtime.RuntimeSetupProgress
import com.zhousl.aether.runtime.SharedPiBridgeClient
import com.zhousl.aether.shared.resources.Res
import com.zhousl.aether.shared.resources.*
import com.zhousl.aether.ui.theme.AetherBackground
import com.zhousl.aether.ui.theme.AetherBackgroundGradientTop
import com.zhousl.aether.ui.theme.AetherOnSurface
import com.zhousl.aether.ui.theme.AetherOnSurfaceVariant
import com.zhousl.aether.ui.theme.AetherPrimary
import com.zhousl.aether.ui.theme.AetherScrim
import com.zhousl.aether.ui.theme.AetherSecondary
import com.zhousl.aether.ui.theme.AetherSurface
import com.zhousl.aether.ui.theme.AetherSurfaceHigh
import com.zhousl.aether.ui.theme.AetherSurfaceHigher
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class SharedRoute { Onboarding, Chat, Settings }
private enum class OnboardingStage { Landing, Runtime, Provider }

internal data class SharedChatMessage(
    val id: String = platformRandomUuid(),
    val text: String,
    val fromUser: Boolean,
    val isError: Boolean = false,
    val reasoningText: String = "",
    val tools: List<SharedChatToolInvocation> = emptyList(),
    val isStreaming: Boolean = false,
    val status: String = "",
    val attachments: List<SharedChatAttachment> = emptyList(),
    val usage: SharedPiUsage? = null,
    val responseGroupId: String = "",
    val isActiveBranch: Boolean = true,
    val branchIndex: Int = 0,
    val branchCount: Int = 1,
)
internal data class SharedPendingTurn(
    val id: String = platformRandomUuid(),
    val text: String,
    val attachments: List<SharedChatAttachment> = emptyList(),
)
private enum class SharedSettingsKind {
    Generic,
    General,
    Providers,
    Personalization,
    WebTools,
    Reliability,
    Skills,
    Extensions,
    Mcp,
    Alpine,
    Terminal,
    Chrome,
    Statistics,
    Developer,
    About,
}
private data class SettingsDestination(
    val title: String,
    val subtitle: String,
    val kind: SharedSettingsKind = SharedSettingsKind.Generic,
)

private val TopFadeHeight = 42.dp
private val ComposerShape = RoundedCornerShape(26.dp)
private val ControlShadow = Color(0x14000000)
private val ComposerShadow = Color(0x18000000)
private val ComposerPurple = Color(0xFF9B5CFF)

@Composable
fun AetherSharedApp(
    runtime: MultiplatformLocalRuntime,
    capabilities: PlatformCapabilities,
    settingsStore: AetherSettingsStore? = null,
    chatHistoryDatabase: ChatHistoryDatabase? = null,
    platformServices: PlatformServices = NoOpPlatformServices,
) {
    var sharedAppSettings by remember { mutableStateOf(AppSettings()) }
    LaunchedEffect(settingsStore) {
        settingsStore?.load()?.let { sharedAppSettings = it.appSettings }
    }
    applyPlatformAppLanguage(sharedAppSettings.language)
    SharedAetherTheme(themeMode = sharedAppSettings.themeMode) {
        val appScope = rememberCoroutineScope()
        val bridgeClient = remember(runtime) {
            SharedPiBridgeClient(RuntimePiBridgeTransport(runtime))
        }
        val mcpManager = remember(runtime) { SharedMcpManager(runtime) }
        val chromeManager = remember(runtime) { SharedChromeManager(runtime) }
        val hostToolRegistry = remember(runtime, mcpManager, chromeManager) {
            SharedCompositeHostTools(
                listOf(
                    SharedToolRegistry(RuntimeHostToolExecutor(runtime), mcpManager),
                    chromeManager,
                    SharedWebToolExecutor(settings = { sharedAppSettings }),
                )
            )
        }
        val chatClient = remember(bridgeClient, hostToolRegistry) {
            SharedPiChatClient(bridgeClient, hostToolRegistry)
        }
        val skillManager = remember(runtime) { SharedSkillManager(runtime) }
        val installedSkills = remember { mutableStateListOf<SharedInstalledSkill>() }
        val selectedSkillIds = remember { mutableStateListOf<String>() }
        val mcpServers = remember { mutableStateListOf<SharedMcpServerConfig>() }
        val activeMcpServerIds = remember { mutableStateListOf<String>() }
        var chromeEnabled by rememberSaveable { mutableStateOf(false) }
        DisposableEffect(bridgeClient) {
            onDispose { appScope.launch { bridgeClient.close() } }
        }
        var route by rememberSaveable { mutableStateOf(SharedRoute.Onboarding) }
        var providerConfig by remember { mutableStateOf<LlmProviderConfig?>(null) }
        val historyStore = remember(chatHistoryDatabase) {
            chatHistoryDatabase?.let(::SharedChatHistoryStore)
        }
        val sessions = remember { mutableStateListOf<SharedConversationSummary>() }
        val messages = remember { mutableStateListOf<SharedChatMessage>() }
        val queuedTurns = remember { mutableStateListOf<SharedPendingTurn>() }
        var input by rememberSaveable { mutableStateOf("") }
        var chatJob by remember { mutableStateOf<Job?>(null) }
        var backgroundLease by remember { mutableStateOf<BackgroundExecutionLease?>(null) }
        var streamingStatus by remember { mutableStateOf("") }
        var sessionId by rememberSaveable { mutableStateOf("aether-session-${platformRandomUuid()}") }
        var extensionSnapshot by remember { mutableStateOf(SharedAetherExtensionSnapshot()) }
        var activeExtensionPageId by rememberSaveable { mutableStateOf("") }
        val backgroundExecutionManager = remember { createBackgroundExecutionManager() }

        suspend fun persistCurrentSession() {
            historyStore?.save(
                sessionId = sessionId,
                messages = messages.toPersistedMessages(),
                selectedSkillIds = selectedSkillIds.toList(),
                activeMcpServerIds = activeMcpServerIds.toList(),
                chromeEnabled = chromeEnabled,
                selectedModelKey = providerConfig?.modelId.orEmpty(),
            )
        }

        LaunchedEffect(Unit) {
            SharedApplicationLifecycle.backgrounded.collect { backgrounded ->
                if (backgrounded && chatJob?.isActive == true && backgroundLease == null) {
                    backgroundLease = backgroundExecutionManager.begin("Aether chat turn") {
                        appScope.launch {
                            chatJob?.cancel()
                            chatJob = null
                            streamingStatus = "Interrupted"
                            val pending = messages.lastOrNull()
                            if (pending?.fromUser == false) {
                                messages.updateMessage(pending.id) {
                                    it.interruptedByBackgroundExpiration()
                                }
                            }
                            persistCurrentSession()
                            backgroundLease = null
                        }
                    }
                } else if (!backgrounded) {
                    backgroundLease?.end()
                    backgroundLease = null
                }
            }
        }

        LaunchedEffect(settingsStore, historyStore) {
            settingsStore?.load()?.let { persisted ->
                sharedAppSettings = persisted.appSettings
                providerConfig = persisted.activeProviderConfig
                if (shouldRestoreSharedChat(persisted.onboardingCompletedVersion)) {
                    route = SharedRoute.Chat
                }
            }
            val persistedSessions = historyStore?.loadAll().orEmpty()
            sessions.clear()
            sessions.addAll(
                persistedSessions.map { persisted ->
                    SharedConversationSummary(persisted.id, persisted.title)
                }
            )
            persistedSessions.firstOrNull()?.let { persisted ->
                sessionId = persisted.id
                messages.clear()
                messages.addAll(
                    persisted.messages.map(PersistedChatMessage::toSharedChatMessage)
                )
                selectedSkillIds.clear()
                selectedSkillIds.addAll(persisted.selectedSkillIds)
                activeMcpServerIds.clear()
                activeMcpServerIds.addAll(persisted.activeMcpServerIds)
                chromeEnabled = persisted.chromeEnabled && capabilities.alpineChrome
                chromeManager.enabled = chromeEnabled
            }
        }

        LaunchedEffect(route) {
            if (route == SharedRoute.Chat || route == SharedRoute.Settings) {
                runCatching {
                    runtime.initialize()
                    skillManager.list()
                }.onSuccess { skills ->
                    installedSkills.clear()
                    installedSkills.addAll(skills)
                    selectedSkillIds.retainAll(skills.map { it.id }.toSet())
                    if (selectedSkillIds.isEmpty() && messages.isEmpty()) {
                        selectedSkillIds.addAll(
                            sharedAppSettings.defaultSelectedSkillIds.filter {
                                id -> skills.any { it.id == id }
                            }
                        )
                    }
                }
                runCatching {
                    val servers = mcpManager.loadServers()
                    mcpManager.refreshBindings(servers)
                    servers
                }.onSuccess { servers ->
                    mcpServers.clear()
                    mcpServers.addAll(servers)
                    if (activeMcpServerIds.isEmpty()) {
                        activeMcpServerIds.addAll(servers.filter { it.enabled }.map { it.id })
                    } else {
                        activeMcpServerIds.retainAll(servers.map { it.id }.toSet())
                    }
                }
            }
        }

        fun startChatTurn(
            rawValue: String,
            attachments: List<SharedChatAttachment> = emptyList(),
            retryResponseGroupId: String = "",
        ) {
            val value = rawValue.trim()
            val config = providerConfig
            if ((value.isEmpty() && attachments.isEmpty()) || config == null) return
            if (chatJob?.isActive == true) {
                queuedTurns += SharedPendingTurn(text = value, attachments = attachments)
                input = ""
                return
            }
            val userMessage = if (retryResponseGroupId.isBlank()) {
                SharedChatMessage(text = value, fromUser = true, attachments = attachments).also {
                    messages += it
                }
            } else {
                messages.lastOrNull { it.id == retryResponseGroupId && it.fromUser }
                    ?: return
            }
            input = ""
            val assistantId = platformRandomUuid()
            val existingBranches = messages.count {
                !it.fromUser && it.responseGroupId == userMessage.id
            }
            messages += SharedChatMessage(
                id = assistantId,
                text = "",
                fromUser = false,
                isStreaming = true,
                status = "Thinking",
                responseGroupId = userMessage.id,
                branchIndex = existingBranches,
                branchCount = existingBranches + 1,
            )
            streamingStatus = "Thinking"
            chatJob = appScope.launch {
                val turnMessages = messages
                    .filter { it.id != assistantId && !it.isError && it.isActiveBranch }
                    .map { message -> message.toPiChatMessage(runtime) }
                val activeSkillPrompt = runCatching {
                    skillManager.buildPrompt(selectedSkillIds.toSet())
                }.getOrDefault("")
                runCatching {
                    chatClient.runTurn(
                        config = config,
                        messages = turnMessages,
                        sessionId = sessionId,
                        systemPrompt = buildString {
                            append(sharedAppSettings.systemPrompt)
                            if (activeSkillPrompt.isNotBlank()) {
                                append("\n\nThe user activated these Skills for this turn:\n")
                                append(activeSkillPrompt)
                            }
                        },
                        reasoning = sharedAppSettings.reasoningEffort,
                        onAssistantTextDelta = { delta ->
                            messages.updateMessage(assistantId) { current ->
                                current.copy(text = current.text + delta, status = "Responding")
                            }
                            streamingStatus = "Responding"
                        },
                        onAssistantReasoningDelta = { delta ->
                            messages.updateMessage(assistantId) { current ->
                                current.copy(reasoningText = current.reasoningText + delta, status = "Thinking")
                            }
                        },
                        onHostToolStarted = { call ->
                            messages.updateMessage(assistantId) { current ->
                                current.copy(
                                    status = "Working",
                                    tools = current.tools + SharedChatToolInvocation(
                                        id = call.id,
                                        name = call.name,
                                        summary = call.arguments.toolSummary(),
                                    ),
                                )
                            }
                        },
                        onHostToolFinished = { call, result ->
                            messages.updateMessage(assistantId) { current ->
                                current.copy(
                                    tools = current.tools.map { tool ->
                                        if (tool.id == call.id) {
                                            tool.copy(
                                                output = result.outputJson.toolOutputSummary(),
                                                isRunning = false,
                                                isError = result.isError,
                                            )
                                        } else tool
                                    },
                                )
                            }
                        },
                    )
                }.fold(
                    onSuccess = { result ->
                        messages.updateMessage(assistantId) { current ->
                            current.copy(
                                text = current.text.ifBlank { result.assistantText }
                                    .ifBlank { result.errorMessage },
                                reasoningText = current.reasoningText.ifBlank { result.reasoningText },
                                isError = result.errorMessage.isNotBlank(),
                                isStreaming = false,
                                status = "",
                                usage = result.usage.takeIf { it.totalTokens > 0 },
                            )
                        }
                    },
                    onFailure = { error ->
                        if (error !is CancellationException) {
                            messages.updateMessage(assistantId) { current ->
                                current.copy(
                                    text = current.text.ifBlank { error.message ?: "The request failed." },
                                    isError = true,
                                    isStreaming = false,
                                    status = "",
                                    tools = current.tools.map { tool ->
                                        if (tool.isRunning) tool.copy(isRunning = false, isError = true) else tool
                                    },
                                )
                            }
                        }
                    },
                )
                streamingStatus = ""
                backgroundLease?.end()
                backgroundLease = null
                persistCurrentSession()
                val title = messages.firstOrNull { it.fromUser }?.text.orEmpty()
                    .take(80)
                    .ifBlank { "New chat" }
                sessions.removeAll { it.id == sessionId }
                sessions.add(0, SharedConversationSummary(sessionId, title))
                chatJob = null
                queuedTurns.firstOrNull()?.let { next ->
                    queuedTurns.removeAt(0)
                    startChatTurn(next.text, next.attachments)
                }
            }
        }

        val extensionManager = remember(bridgeClient) {
            SharedAetherExtensionManager(bridgeClient) { method, args ->
                when (method) {
                    "app.getState" -> buildJsonObject {
                        put("screen", route.name.lowercase())
                        put("session_id", sessionId)
                        put("draft_input", input)
                        put("is_generating", chatJob?.isActive == true)
                        put("selected_model_key", providerConfig?.modelId.orEmpty())
                    }
                    "app.setDraftInput" -> withContext(Dispatchers.Main) {
                        input = args["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        buildJsonObject { put("updated", true) }
                    }
                    "app.appendDraftInput" -> withContext(Dispatchers.Main) {
                        input += args["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        buildJsonObject { put("updated", true) }
                    }
                    "app.sendMessage" -> withContext(Dispatchers.Main) {
                        args["text"]?.jsonPrimitive?.contentOrNull?.let { input = it }
                        startChatTurn(input)
                        buildJsonObject { put("submitted", true) }
                    }
                    "app.newChat" -> withContext(Dispatchers.Main) {
                        chatJob?.cancel()
                        chatJob = null
                        sessionId = "aether-session-" + platformRandomUuid()
                        messages.clear()
                        queuedTurns.clear()
                        route = SharedRoute.Chat
                        buildJsonObject { put("opened", "chat") }
                    }
                    "app.openScreen" -> withContext(Dispatchers.Main) {
                        val screen = args["screen"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        route = if (screen.equals("settings", true)) SharedRoute.Settings else SharedRoute.Chat
                        buildJsonObject { put("opened", screen) }
                    }
                    "app.notify" -> buildJsonObject {
                        put("notified", true)
                        put("message", args["message"] ?: JsonPrimitive(""))
                    }
                    "settings.get" -> buildJsonObject {
                        put("system_prompt", sharedAppSettings.systemPrompt)
                        put("reasoning_effort", sharedAppSettings.reasoningEffort)
                        put("theme", sharedAppSettings.themeMode.storageValue)
                        put("language", sharedAppSettings.language.storageValue)
                        put("tavily_api_key", sharedAppSettings.tavilyApiKey)
                        put("tavily_base_url", sharedAppSettings.tavilyBaseUrl)
                    }
                    else -> error("Unsupported Aether extension host method on this platform: " + method)
                }
            }
        }

        LaunchedEffect(route, capabilities.scriptExtensions) {
            if (capabilities.scriptExtensions && route != SharedRoute.Onboarding) {
                runCatching { extensionManager.refresh() }
                    .onSuccess { extensionSnapshot = it }
            }
        }

        val extensionController = SharedAetherExtensionUiController(
            snapshot = extensionSnapshot,
            onAction = { extensionId, action, args ->
                appScope.launch {
                    runCatching { extensionManager.invokeAction(extensionId, action, args) }
                        .onSuccess { extensionSnapshot = it }
                }
            },
            onOpenPage = { activeExtensionPageId = it },
        )

        SharedAetherExtensionUiProvider(extensionController) {
        val activeExtensionPage = extensionSnapshot.pages.firstOrNull { it.id == activeExtensionPageId }
        if (activeExtensionPage != null) {
            SharedAetherExtensionPageScreen(
                page = activeExtensionPage,
                onBack = { activeExtensionPageId = "" },
            )
        } else AnimatedContent(
            targetState = route,
            transitionSpec = {
                (fadeIn() + slideInHorizontally { it / 10 }) togetherWith
                    (fadeOut() + slideOutHorizontally { -it / 12 })
            },
            label = "aether-route",
        ) { current ->
            when (current) {
                SharedRoute.Onboarding -> SharedOnboarding(
                    runtime = runtime,
                    bridgeClient = bridgeClient,
                    existingProviderConfig = providerConfig,
                    onProviderConfigured = { config ->
                        providerConfig = config
                        appScope.launch { settingsStore?.saveProvider(config) }
                    },
                    onComplete = {
                        sharedAppSettings = sharedAppSettings.copy(
                            onboardingSeenVersion = CurrentOnboardingVersion,
                            onboardingCompletedVersion = CurrentOnboardingVersion,
                        )
                        appScope.launch { settingsStore?.markOnboardingComplete() }
                        route = SharedRoute.Chat
                    },
                )
                SharedRoute.Chat -> SharedChatScreen(
                    sessions = sessions,
                    selectedSessionId = sessionId,
                    messages = messages,
                    pendingTurns = queuedTurns,
                    runtime = runtime,
                    platformServices = platformServices,
                    availableSkills = installedSkills,
                    selectedSkillIds = selectedSkillIds,
                    onSkillSelected = { skillId, selected ->
                        if (selected) {
                            if (skillId !in selectedSkillIds) selectedSkillIds += skillId
                        } else {
                            selectedSkillIds.remove(skillId)
                        }
                        appScope.launch { persistCurrentSession() }
                    },
                    mcpServers = mcpServers.filter { it.enabled },
                    activeMcpServerIds = activeMcpServerIds,
                    onMcpServerSelected = { serverId, selected ->
                        if (selected) {
                            if (serverId !in activeMcpServerIds) activeMcpServerIds += serverId
                        } else {
                            activeMcpServerIds.remove(serverId)
                        }
                        appScope.launch {
                            mcpManager.refreshBindings(
                                mcpServers.filter { it.enabled && it.id in activeMcpServerIds }
                            )
                            persistCurrentSession()
                        }
                    },
                    chromeAvailable = capabilities.alpineChrome,
                    chromeEnabled = chromeEnabled,
                    onChromeSelected = { selected ->
                        chromeEnabled = selected && capabilities.alpineChrome
                        chromeManager.enabled = chromeEnabled
                        appScope.launch { persistCurrentSession() }
                    },
                    input = input,
                    isSending = chatJob?.isActive == true,
                    streamingStatus = streamingStatus,
                    selectedModel = providerConfig?.modelId.orEmpty(),
                    onInputChanged = { input = it },
                    onSend = { attachments -> startChatTurn(input, attachments) },
                    onRetry = {
                        messages.lastOrNull { it.fromUser }?.let { user ->
                            messages.indices.forEach { index ->
                                val message = messages[index]
                                if (!message.fromUser && message.responseGroupId == user.id) {
                                    messages[index] = message.copy(isActiveBranch = false)
                                }
                            }
                            startChatTurn(
                                rawValue = user.text,
                                attachments = user.attachments,
                                retryResponseGroupId = user.id,
                            )
                        }
                    },
                    onQueueFollowUp = { attachments ->
                        startChatTurn(input, attachments)
                    },
                    onSteerFollowUp = { attachments ->
                        val value = input.trim()
                        if (value.isNotBlank() || attachments.isNotEmpty()) {
                            input = ""
                            appScope.launch {
                                val userMessage = SharedChatMessage(
                                    text = value,
                                    fromUser = true,
                                    attachments = attachments,
                                )
                                val accepted = runCatching {
                                    chatClient.steer(sessionId, userMessage.toPiChatMessage(runtime))
                                }.getOrDefault(false)
                                if (accepted) {
                                    val assistantIndex = messages.indexOfLast { !it.fromUser && it.isStreaming }
                                    if (assistantIndex >= 0) messages.add(assistantIndex, userMessage)
                                    else messages += userMessage
                                } else {
                                    queuedTurns += SharedPendingTurn(text = value, attachments = attachments)
                                }
                            }
                        }
                    },
                    onEditUserMessage = { messageId ->
                        val index = messages.indexOfFirst { it.id == messageId && it.fromUser }
                        if (index >= 0) {
                            chatJob?.cancel()
                            chatJob = null
                            streamingStatus = ""
                            input = messages[index].text
                            while (messages.size > index) messages.removeAt(messages.lastIndex)
                            appScope.launch {
                                persistCurrentSession()
                            }
                        }
                    },
                    onSelectResponseBranch = { groupId, branchIndex ->
                        val updated = messages.toList().selectSharedResponseBranch(groupId, branchIndex)
                        if (updated !== messages) {
                            messages.clear()
                            messages.addAll(updated)
                            appScope.launch { persistCurrentSession() }
                        }
                    },
                    onStop = {
                        chatJob?.cancel()
                        chatJob = null
                        streamingStatus = ""
                        messages.lastOrNull { !it.fromUser && it.isStreaming }?.let { pending ->
                            messages.updateMessage(pending.id) { current ->
                                current.copy(
                                    isStreaming = false,
                                    status = "Stopped",
                                    tools = current.tools.map { tool ->
                                        if (tool.isRunning) tool.copy(isRunning = false, isError = true) else tool
                                    },
                                )
                            }
                        }
                        backgroundLease?.end()
                        backgroundLease = null
                        appScope.launch {
                            persistCurrentSession()
                        }
                    },
                    onNewChat = {
                        chatJob?.cancel()
                        chatJob = null
                        streamingStatus = ""
                        backgroundLease?.end()
                        backgroundLease = null
                        sessionId = "aether-session-${platformRandomUuid()}"
                        messages.clear()
                        queuedTurns.clear()
                    },
                    onSessionSelected = { selectedId ->
                        if (selectedId != sessionId) {
                            chatJob?.cancel()
                            chatJob = null
                            streamingStatus = ""
                            appScope.launch {
                                historyStore?.load(selectedId)?.let { persisted ->
                                    sessionId = persisted.id
                                    messages.clear()
                                    messages.addAll(
                                        persisted.messages.map(PersistedChatMessage::toSharedChatMessage)
                                    )
                                    selectedSkillIds.clear()
                                    selectedSkillIds.addAll(persisted.selectedSkillIds)
                                    activeMcpServerIds.clear()
                                    activeMcpServerIds.addAll(persisted.activeMcpServerIds)
                                    chromeEnabled = persisted.chromeEnabled && capabilities.alpineChrome
                                    chromeManager.enabled = chromeEnabled
                                }
                            }
                        }
                    },
                    onRenameSession = { selectedId, title ->
                        appScope.launch {
                            historyStore?.rename(selectedId, title)
                            val index = sessions.indexOfFirst { it.id == selectedId }
                            if (index >= 0) sessions[index] = sessions[index].copy(title = title)
                        }
                    },
                    onDeleteSession = { selectedId ->
                        appScope.launch {
                            historyStore?.delete(selectedId)
                            sessions.removeAll { it.id == selectedId }
                            if (sessionId == selectedId) {
                                val next = sessions.firstOrNull()?.let { historyStore?.load(it.id) }
                                if (next == null) {
                                    sessionId = "aether-session-${platformRandomUuid()}"
                                    messages.clear()
                                } else {
                                    sessionId = next.id
                                    messages.clear()
                                    messages.addAll(
                                        next.messages.map(PersistedChatMessage::toSharedChatMessage)
                                    )
                                }
                            }
                        }
                    },
                    onOpenSettings = { route = SharedRoute.Settings },
                )
                SharedRoute.Settings -> SharedSettingsScreen(
                    capabilities = capabilities,
                    runtime = runtime,
                    platformServices = platformServices,
                    providerConfig = providerConfig,
                    appSettings = sharedAppSettings,
                    usage = messages.mapNotNull { it.usage },
                    bridgeClient = bridgeClient,
                    skillManager = skillManager,
                    installedSkills = installedSkills,
                    mcpManager = mcpManager,
                    mcpServers = mcpServers,
                    onMcpServersChanged = { servers ->
                        mcpServers.clear()
                        mcpServers.addAll(servers)
                    },
                    chromeManager = chromeManager,
                    onSkillsChanged = { skills ->
                        installedSkills.clear()
                        installedSkills.addAll(skills)
                    },
                    onProviderSaved = { config ->
                        providerConfig = config
                        appScope.launch { settingsStore?.saveProvider(config) }
                    },
                    onGeneralSettingsSaved = { updated ->
                        sharedAppSettings = updated
                        appScope.launch { settingsStore?.saveGeneralSettings(updated) }
                    },
                    onBack = { route = SharedRoute.Chat },
                    onReplayOnboarding = { route = SharedRoute.Onboarding },
                )
            }
        }
        SharedAetherExtensionOverlay(Modifier.fillMaxSize())
        }
    }
}

internal fun shouldRestoreSharedChat(onboardingCompletedVersion: Int): Boolean =
    onboardingCompletedVersion > 0

private val Base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

private suspend fun SharedChatMessage.toPiChatMessage(
    runtime: MultiplatformLocalRuntime,
): SharedPiChatMessage {
    val imageParts = attachments
        .filter { it.mimeType.startsWith("image/") }
        .mapNotNull { attachment ->
            runCatching {
                SharedPiImage(
                    mimeType = attachment.mimeType,
                    data = runtime.fileSystem.read(attachment.workspacePath).encodeBase64(),
                )
            }.getOrNull()
        }
    return SharedPiChatMessage(
        role = if (fromUser) "user" else "assistant",
        text = buildString {
            append(text.ifBlank { "Please analyze the attached file(s)." })
            if (attachments.isNotEmpty()) {
                append("\n\nAttached workspace files:\n")
                attachments.forEach { attachment ->
                    append("- ")
                    append(attachment.workspacePath)
                    append(" (")
                    append(attachment.mimeType)
                    append(")\n")
                }
            }
        },
        images = imageParts,
    )
}

private fun ByteArray.encodeBase64(): String = buildString(((size + 2) / 3) * 4) {
    var index = 0
    while (index < size) {
        val first = this@encodeBase64[index++].toInt() and 0xff
        val hasSecond = index < size
        val second = if (hasSecond) this@encodeBase64[index++].toInt() and 0xff else 0
        val hasThird = index < size
        val third = if (hasThird) this@encodeBase64[index++].toInt() and 0xff else 0
        append(Base64Alphabet[first ushr 2])
        append(Base64Alphabet[((first and 0x03) shl 4) or (second ushr 4)])
        append(if (hasSecond) Base64Alphabet[((second and 0x0f) shl 2) or (third ushr 6)] else '=')
        append(if (hasThird) Base64Alphabet[third and 0x3f] else '=')
    }
}

@Composable
private fun SharedOnboarding(
    runtime: MultiplatformLocalRuntime,
    bridgeClient: SharedPiBridgeClient,
    existingProviderConfig: LlmProviderConfig?,
    onProviderConfigured: (LlmProviderConfig) -> Unit,
    onComplete: () -> Unit,
) {
    var stage by rememberSaveable { mutableStateOf(OnboardingStage.Landing) }
    when (stage) {
        OnboardingStage.Landing -> OnboardingLandingStep(
            stepIndex = 1,
            stepCount = 3,
            replayMode = false,
            onPrimary = { stage = OnboardingStage.Runtime },
            onSecondary = onComplete,
        )
        OnboardingStage.Runtime -> RuntimeSetupStep(
            runtime = runtime,
            onBack = { stage = OnboardingStage.Landing },
            onContinue = { stage = OnboardingStage.Provider },
        )
        OnboardingStage.Provider -> SharedProviderSetupStep(
            bridgeClient = bridgeClient,
            existingProviderConfig = existingProviderConfig,
            onBack = { stage = OnboardingStage.Runtime },
            onSkip = onComplete,
            onComplete = { config ->
                onProviderConfigured(config)
                onComplete()
            },
        )
    }
}

@Composable
private fun SharedProviderSetupStep(
    bridgeClient: SharedPiBridgeClient,
    existingProviderConfig: LlmProviderConfig?,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onComplete: (LlmProviderConfig) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val formState = rememberProviderFormState(existingProviderConfig)
    val modelCatalogClient = remember { SharedProviderModelCatalogClient() }
    var authState by remember { mutableStateOf(PiProviderAuthState()) }
    var fetchingModels by remember { mutableStateOf(false) }

    fun clearAuthState() {
        authState = PiProviderAuthState()
    }

    OnboardingConversationStepPage(
        stepIndex = 3,
        stepCount = 3,
        message = stringResource(Res.string.onboarding_provider_auth_message),
        onBack = onBack,
        topRightLabel = stringResource(Res.string.common_skip),
        onTopRight = onSkip,
    ) {
        AddProviderWizard(
            state = formState,
            existingProviderIds = emptySet(),
            isFetchingModels = fetchingModels,
            onFetchModels = { config, callback ->
                fetchingModels = true
                scope.launch {
                    val models = runCatching {
                        modelCatalogClient.fetchModels(config, bridgeClient::listProviders).models
                    }.getOrDefault(emptyList())
                        .ifEmpty {
                            listOf(PiProviderCatalog.resolve(config.piProviderId).defaultModelId)
                                .filter(String::isNotBlank)
                        }
                    fetchingModels = false
                    callback(models)
                }
            },
            authState = authState,
            onStartProviderLogin = { configId, providerId, authMethod, oauthFlow ->
                authState = PiProviderAuthState(
                    providerId = providerId,
                    authMethod = authMethod,
                    isRunning = true,
                    statusMessage = if (authMethod == ProviderAuthMethod.OAuth) {
                        "Waiting for authorization."
                    } else {
                        "Waiting for credentials."
                    },
                )
                scope.launch {
                    runCatching {
                        bridgeClient.loginProvider(
                            providerConfigId = configId,
                            providerId = providerId,
                            authMethod = authMethod.storageValue,
                            oauthFlow = oauthFlow,
                        ) { event, payload ->
                            authState = authState.withBridgeAuthEvent(event, payload)
                        }
                    }.fold(
                        onSuccess = { payload ->
                            authState = authState.copy(
                                isRunning = false,
                                prompt = null,
                                apiKey = payload.string("api_key"),
                                oauthCredentialJson = (payload["oauth_credential"] as? JsonObject)
                                    ?.toString()
                                    .orEmpty(),
                                providerEnvironmentVariables = payload.toPiProviderEnvironmentVariables(),
                                statusMessage = if (authMethod == ProviderAuthMethod.OAuth) {
                                    "Connected with OAuth."
                                } else {
                                    "API key configured."
                                },
                                errorMessage = "",
                            )
                        },
                        onFailure = { error ->
                            authState = authState.copy(
                                isRunning = false,
                                prompt = null,
                                statusMessage = "",
                                errorMessage = error.message.orEmpty(),
                            )
                        },
                    )
                }
            },
            onSubmitAuthPrompt = { promptId, value, cancelled ->
                scope.launch {
                    runCatching { bridgeClient.submitAuthPrompt(promptId, value, cancelled) }
                    authState = authState.copy(prompt = null)
                }
            },
            onClearAuthState = ::clearAuthState,
            onSave = onComplete,
        )
    }
}

private fun PiProviderAuthState.withBridgeAuthEvent(event: String, payload: JsonObject): PiProviderAuthState =
    when (event) {
        "auth_url" -> copy(
            authorizationUrl = payload.string("url"),
            statusMessage = payload.string("instructions").ifBlank {
                "Complete authorization in your browser."
            },
        )
        "auth_device_code" -> copy(
            deviceCode = payload.string("user_code"),
            verificationUrl = payload.string("verification_uri"),
            statusMessage = "Enter the device code in your browser.",
        )
        "auth_prompt" -> copy(
            prompt = payload.toPiOAuthPrompt(),
            statusMessage = payload.string("message"),
        )
        "auth_progress" -> copy(statusMessage = payload.string("message"))
        else -> this
    }

private fun JsonObject.string(name: String): String =
    get(name)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.toolSummary(): String = sequenceOf(
    string("path"),
    string("command"),
    string("pattern"),
    string("duration_ms"),
).firstOrNull { it.isNotBlank() }.orEmpty().take(180)

private fun String.toolOutputSummary(): String = runCatching {
    val payload = Json.parseToJsonElement(this) as? JsonObject ?: return@runCatching this
    sequenceOf(
        payload.string("stdout"),
        payload.string("stderr"),
        payload.string("error"),
        payload.string("path"),
    ).firstOrNull { it.isNotBlank() }.orEmpty()
}.getOrDefault(this).take(12_000)

@Composable
private fun RuntimeSetupStep(
    runtime: MultiplatformLocalRuntime,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    var retryKey by rememberSaveable { mutableIntStateOf(0) }
    var ready by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    var progress by remember { mutableStateOf(RuntimeSetupProgress("idle")) }
    var running by remember { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(retryKey) {
        running = true
        error = ""
        runCatching { runtime.initialize { progress = it } }
            .onSuccess { ready = true }
            .onFailure { error = it.message.orEmpty() }
        running = false
    }

    OnboardingConversationStepPage(
        stepIndex = 2,
        stepCount = 3,
        message = stringResource(Res.string.runtime_setup_message),
        onBack = onBack,
        topRightLabel = stringResource(Res.string.close_label),
        onTopRight = onBack,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            OnboardingStepLead(
                icon = Icons.Rounded.Code,
                accent = when {
                    ready -> AetherSecondary
                    error.isNotBlank() -> MaterialTheme.colorScheme.error
                    else -> AetherPrimary
                },
                title = stringResource(Res.string.alpine_title),
                body = when {
                    error.isNotBlank() -> error.ifBlank { stringResource(Res.string.runtime_error) }
                    ready -> stringResource(Res.string.alpine_ready)
                    else -> progress.detail.ifBlank { stringResource(Res.string.alpine_preparing) }
                },
            )
            RuntimeSetupProgressPanel(
                progress = progress,
                ready = ready,
                error = error,
                onShowDetails = { showDetails = true },
            )
            if (running && progress.fraction == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 1.8.dp)
                    Text(
                        text = progress.detail.ifBlank { stringResource(Res.string.alpine_preparing) },
                        style = MaterialTheme.typography.bodySmall,
                        color = AetherOnSurfaceVariant,
                    )
                }
            }
            OnboardingActionRow(
                primaryLabel = stringResource(
                    when {
                        ready -> Res.string.continue_label
                        error.isNotBlank() -> Res.string.retry_label
                        else -> Res.string.alpine_preparing
                    },
                ),
                onPrimary = if (ready) onContinue else ({ retryKey += 1 }),
                primaryEnabled = ready || error.isNotBlank(),
                primaryLoading = running,
                secondaryLabel = stringResource(Res.string.back_label),
                onSecondary = onBack,
            )
        }
    }
    if (showDetails) {
        RuntimeSetupDetailsDialog(
            output = progress.output,
            onDismiss = { showDetails = false },
        )
    }
}

@Composable
private fun RuntimeSetupProgressPanel(
    progress: RuntimeSetupProgress,
    ready: Boolean,
    error: String,
    onShowDetails: () -> Unit,
) {
    val fraction = when {
        ready -> 1f
        else -> progress.fraction?.coerceIn(0f, 1f) ?: 0f
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AetherSurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            text = when {
                error.isNotBlank() -> error
                ready -> stringResource(Res.string.alpine_ready)
                else -> progress.detail.ifBlank { stringResource(Res.string.alpine_preparing) }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (error.isNotBlank()) MaterialTheme.colorScheme.error else AetherOnSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = AetherOnSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.runtime_setup_details),
                modifier = Modifier.clickable(onClick = onShowDetails).padding(vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = AetherPrimary,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AetherSurfaceHigher),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (ready) AetherSecondary else AetherPrimary),
                )
            }
        }
    }
}

@Composable
private fun RuntimeSetupDetailsDialog(
    output: String,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(output) { scrollState.scrollTo(scrollState.maxValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.runtime_setup_details_title)) },
        text = {
            Text(
                text = output.ifBlank { stringResource(Res.string.runtime_setup_waiting_for_output) },
                modifier = Modifier.fillMaxWidth().height(320.dp).verticalScroll(scrollState),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = AetherOnSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close_label)) }
        },
        shape = RoundedCornerShape(8.dp),
        containerColor = AetherSurface,
    )
}

@Composable
private fun SharedChatScreen(
    sessions: List<SharedConversationSummary>,
    selectedSessionId: String,
    messages: List<SharedChatMessage>,
    pendingTurns: List<SharedPendingTurn>,
    runtime: MultiplatformLocalRuntime,
    platformServices: PlatformServices,
    availableSkills: List<SharedInstalledSkill>,
    selectedSkillIds: List<String>,
    onSkillSelected: (String, Boolean) -> Unit,
    mcpServers: List<SharedMcpServerConfig>,
    activeMcpServerIds: List<String>,
    onMcpServerSelected: (String, Boolean) -> Unit,
    chromeAvailable: Boolean,
    chromeEnabled: Boolean,
    onChromeSelected: (Boolean) -> Unit,
    input: String,
    isSending: Boolean,
    streamingStatus: String,
    selectedModel: String,
    onInputChanged: (String) -> Unit,
    onSend: (List<SharedChatAttachment>) -> Unit,
    onRetry: () -> Unit,
    onQueueFollowUp: (List<SharedChatAttachment>) -> Unit,
    onSteerFollowUp: (List<SharedChatAttachment>) -> Unit,
    onEditUserMessage: (String) -> Unit,
    onSelectResponseBranch: (String, Int) -> Unit,
    onStop: () -> Unit,
    onNewChat: () -> Unit,
    onSessionSelected: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val visibleMessages = messages.filter { it.fromUser || it.isActiveBranch }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AetherConversationDrawer(
                sessions = sessions,
                selectedSessionId = selectedSessionId,
                onNewChat = {
                    onNewChat()
                    scope.launch { drawerState.close() }
                },
                onSessionSelected = { id ->
                    onSessionSelected(id)
                    scope.launch { drawerState.close() }
                },
                onRenameSession = onRenameSession,
                onExportSession = {},
                onDeleteSession = onDeleteSession,
                onSettingsSelected = {
                    scope.launch {
                        drawerState.close()
                        onOpenSettings()
                    }
                },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AetherBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(AetherBackgroundGradientTop, AetherBackground, AetherSurface))
                ).padding(innerPadding),
            ) {
                if (visibleMessages.isEmpty()) {
                    AetherConversationEmptyState(
                        modifier = Modifier.fillMaxSize().padding(top = 96.dp, bottom = 118.dp),
                        welcomeLabel = stringResource(Res.string.chat_welcome_help),
                        analyzeImageLabel = stringResource(Res.string.chat_analyze_image_chip),
                        codeLabel = stringResource(Res.string.chat_code_chip),
                        helpWriteLabel = stringResource(Res.string.chat_help_me_write_chip),
                        summarizeFileLabel = stringResource(Res.string.chat_summarize_file_chip),
                        inputFocused = false,
                        onStarterPromptSelected = onInputChanged,
                    )
                    SharedAetherExtensionSlot(
                        SharedExtensionSlotChatEmpty,
                        Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 108.dp, bottom = 132.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        item {
                            SharedAetherExtensionSlot(SharedExtensionSlotChatListStart)
                        }
                        itemsIndexed(visibleMessages, key = { _, message -> message.id }) { index, rawMessage ->
                            val branches = if (rawMessage.responseGroupId.isBlank()) {
                                emptyList()
                            } else {
                                messages.filter {
                                    !it.fromUser && it.responseGroupId == rawMessage.responseGroupId
                                }
                            }
                            val branchIndex = branches.indexOfFirst { it.id == rawMessage.id }
                                .coerceAtLeast(0)
                            val message = rawMessage.copy(
                                branchIndex = branchIndex,
                                branchCount = branches.size.coerceAtLeast(1),
                            )
                            SharedConversationMessage(
                                message = message,
                                canRetry = !message.fromUser && index == visibleMessages.lastIndex && !isSending,
                                onRetry = onRetry,
                                onCopy = platformServices::copyText,
                                onShare = { text -> platformServices.shareText("Aether", text) },
                                onEdit = { onEditUserMessage(message.id) },
                                onPreviousBranch = {
                                    onSelectResponseBranch(message.responseGroupId, message.branchIndex - 1)
                                },
                                onNextBranch = {
                                    onSelectResponseBranch(message.responseGroupId, message.branchIndex + 1)
                                },
                            )
                        }
                        items(pendingTurns, key = SharedPendingTurn::id) { pending ->
                            SharedPendingInputBubble(pending)
                        }
                        item {
                            SharedAetherExtensionSlot(SharedExtensionSlotChatListEnd)
                        }
                    }
                }
                SharedAetherExtensionSlot(
                    SharedExtensionSlotChatTop,
                    Modifier.align(Alignment.TopCenter).padding(top = 102.dp, start = 20.dp, end = 20.dp),
                )
                ConversationTopBar(
                    modifier = Modifier.align(Alignment.TopCenter),
                    onMenu = { scope.launch { drawerState.open() } },
                    onNewChat = onNewChat,
                    selectedModel = selectedModel,
                )
                SharedComposer(
                    value = input,
                    onValueChange = onInputChanged,
                    onSend = onSend,
                    isSending = isSending,
                    onStop = onStop,
                    onQueueFollowUp = onQueueFollowUp,
                    onSteerFollowUp = onSteerFollowUp,
                    runtime = runtime,
                    platformServices = platformServices,
                    availableSkills = availableSkills,
                    selectedSkillIds = selectedSkillIds,
                    onSkillSelected = onSkillSelected,
                    mcpServers = mcpServers,
                    activeMcpServerIds = activeMcpServerIds,
                    onMcpServerSelected = onMcpServerSelected,
                    chromeAvailable = chromeAvailable,
                    chromeEnabled = chromeEnabled,
                    onChromeSelected = onChromeSelected,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun ConversationTopBar(
    modifier: Modifier,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    selectedModel: String,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AetherConversationTopBarFrame(
            modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(AetherBackgroundGradientTop, AetherBackground.copy(alpha = 0.98f)))
            ).statusBarsPadding(),
            menuDescription = stringResource(Res.string.menu_label),
            newChatDescription = stringResource(Res.string.new_chat),
            onMenu = onMenu,
            onNewChat = onNewChat,
        ) {
            AetherSimpleModelSelector(
                label = selectedModel.ifBlank { stringResource(Res.string.automatic_model) },
            )
        }
        Spacer(
            modifier = Modifier.fillMaxWidth().height(TopFadeHeight).background(
                Brush.verticalGradient(listOf(AetherBackground.copy(alpha = 0.98f), Color.Transparent))
            )
        )
    }
}

@Composable
private fun SharedComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: (List<SharedChatAttachment>) -> Unit,
    isSending: Boolean,
    onStop: () -> Unit,
    onQueueFollowUp: (List<SharedChatAttachment>) -> Unit,
    onSteerFollowUp: (List<SharedChatAttachment>) -> Unit,
    runtime: MultiplatformLocalRuntime,
    platformServices: PlatformServices,
    availableSkills: List<SharedInstalledSkill>,
    selectedSkillIds: List<String>,
    onSkillSelected: (String, Boolean) -> Unit,
    mcpServers: List<SharedMcpServerConfig>,
    activeMcpServerIds: List<String>,
    onMcpServerSelected: (String, Boolean) -> Unit,
    chromeAvailable: Boolean,
    chromeEnabled: Boolean,
    onChromeSelected: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val attachments = remember { mutableStateListOf<SharedChatAttachment>() }
    var menuOpen by remember { mutableStateOf(false) }
    var skillsOpen by remember { mutableStateOf(false) }
    var mcpOpen by remember { mutableStateOf(false) }
    var attachmentError by remember { mutableStateOf("") }
    var followUpMenuOpen by remember { mutableStateOf(false) }

    fun pickAttachment(imagesOnly: Boolean) {
        menuOpen = false
        scope.launch {
            attachmentError = ""
            runCatching {
                val selected = platformServices.pickFile(imagesOnly) ?: return@runCatching
                val safeName = selected.name
                    .replace(Regex("[^A-Za-z0-9._-]+"), "-")
                    .trim('-')
                    .ifBlank { "attachment" }
                val path = "${runtime.workspaceRoot.trimEnd('/')}/attachments/${platformRandomUuid()}-$safeName"
                runtime.fileSystem.createDirectories("${runtime.workspaceRoot.trimEnd('/')}/attachments")
                runtime.fileSystem.write(path, selected.bytes)
                attachments += SharedChatAttachment(
                    id = platformRandomUuid(),
                    name = selected.name,
                    mimeType = selected.mimeType,
                    workspacePath = path,
                    sizeBytes = selected.bytes.size.toLong(),
                )
            }.onFailure { attachmentError = it.message ?: "Unable to attach file." }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Color.Transparent, AetherSurface.copy(alpha = 0.94f)))
        ).imePadding().navigationBarsPadding().padding(horizontal = 30.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            SharedAetherExtensionSlot(
                SharedExtensionSlotChatComposerTop,
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            if (menuOpen) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AetherSurface)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ComposerPickerAction(Icons.Rounded.Image, "Photo") { pickAttachment(true) }
                    ComposerPickerAction(Icons.Rounded.AttachFile, "File") { pickAttachment(false) }
                    if (availableSkills.isNotEmpty()) {
                        ComposerPickerAction(Icons.Rounded.AutoAwesome, "Skills") {
                            skillsOpen = !skillsOpen
                            menuOpen = false
                        }
                    }
                    if (mcpServers.isNotEmpty()) {
                        ComposerPickerAction(Icons.Rounded.Code, "MCP") {
                            mcpOpen = !mcpOpen
                            menuOpen = false
                        }
                    }
                    if (chromeAvailable) {
                        ComposerPickerAction(Icons.Rounded.Public, if (chromeEnabled) "Chrome on" else "Chrome") {
                            onChromeSelected(!chromeEnabled)
                            menuOpen = false
                        }
                    }
                }
            }
            if (skillsOpen && availableSkills.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp)).background(AetherSurface).padding(7.dp),
                ) {
                    availableSkills.forEach { skill ->
                        val selected = skill.id in selectedSkillIds
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .clickable { onSkillSelected(skill.id, !selected) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                null,
                                tint = if (selected) ComposerPurple else AetherOnSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                skill.name,
                                modifier = Modifier.weight(1f),
                                color = AetherOnSurface,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (selected) Icon(Icons.Rounded.Check, null, tint = ComposerPurple, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            if (mcpOpen && mcpServers.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp)).background(AetherSurface).padding(7.dp),
                ) {
                    mcpServers.forEach { server ->
                        val selected = server.id in activeMcpServerIds
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .clickable { onMcpServerSelected(server.id, !selected) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Code, null, tint = if (selected) ComposerPurple else AetherOnSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(server.name, modifier = Modifier.weight(1f), color = AetherOnSurface, style = MaterialTheme.typography.labelLarge)
                            if (selected) Icon(Icons.Rounded.Check, null, tint = ComposerPurple, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            if (attachments.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    attachments.forEach { attachment ->
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(AetherSurfaceHigh)
                                .padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                attachment.name,
                                color = AetherOnSurface,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 180.dp),
                            )
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape)
                                    .clickable { attachments.removeAll { it.id == attachment.id } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Close, "Remove", modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
            if (attachmentError.isNotBlank()) {
                Text(
                    attachmentError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().shadow(12.dp, ComposerShape, ambientColor = ComposerShadow, spotColor = ComposerShadow)
                    .clip(ComposerShape).background(AetherSurface).padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).clickable { menuOpen = !menuOpen },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = stringResource(Res.string.chat_add_attachment),
                    tint = AetherOnSurface,
                    modifier = Modifier.size(27.dp),
                )
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                if (value.isBlank()) {
                    Text(
                        stringResource(Res.string.message_hint),
                        color = AetherOnSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = AetherOnSurface, fontSize = 16.sp, lineHeight = 22.sp),
                    cursorBrush = SolidColor(AetherOnSurface),
                    maxLines = 5,
                )
            }
            if (isSending) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(AetherSurfaceHigh)
                        .clickable(onClick = onStop),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.common_stop),
                        tint = AetherOnSurface,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            if (value.isNotBlank() || attachments.isNotEmpty()) {
                Box {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(ComposerPurple)
                            .clickable {
                                if (isSending) {
                                    followUpMenuOpen = true
                                } else {
                                    onSend(attachments.toList())
                                    attachments.clear()
                                    menuOpen = false
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = stringResource(Res.string.send_label),
                            tint = Color.White,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = followUpMenuOpen,
                        onDismissRequest = { followUpMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Steer current run") },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null) },
                            onClick = {
                                followUpMenuOpen = false
                                onSteerFollowUp(attachments.toList())
                                attachments.clear()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Queue next turn") },
                            leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                            onClick = {
                                followUpMenuOpen = false
                                onQueueFollowUp(attachments.toList())
                                attachments.clear()
                            },
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ComposerPickerAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = AetherOnSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = AetherOnSurface, style = MaterialTheme.typography.labelLarge)
    }
}

private inline fun androidx.compose.runtime.snapshots.SnapshotStateList<SharedChatMessage>.updateMessage(
    id: String,
    transform: (SharedChatMessage) -> SharedChatMessage,
) {
    val index = indexOfFirst { it.id == id }
    if (index >= 0) this[index] = transform(this[index])
}

internal fun List<SharedChatMessage>.selectSharedResponseBranch(
    groupId: String,
    branchIndex: Int,
): List<SharedChatMessage> {
    val branches = filter { !it.fromUser && it.responseGroupId == groupId }
    val selectedId = branches.getOrNull(branchIndex)?.id ?: return this
    return map { message ->
        if (!message.fromUser && message.responseGroupId == groupId) {
            message.copy(
                isActiveBranch = message.id == selectedId,
                branchIndex = branches.indexOfFirst { it.id == message.id },
                branchCount = branches.size,
            )
        } else message
    }
}

internal fun SharedChatMessage.interruptedByBackgroundExpiration(): SharedChatMessage = copy(
    text = text.ifBlank {
        "This response was interrupted when iOS background time expired. Retry the message to continue."
    },
    isError = text.isBlank(),
    isStreaming = false,
    status = "Interrupted",
    tools = tools.map { tool ->
        if (tool.isRunning) tool.copy(isRunning = false, isError = true) else tool
    },
)

private fun List<SharedChatMessage>.toPersistedMessages(): List<PersistedChatMessage> = map { message ->
    PersistedChatMessage(
        id = message.id,
        text = message.text,
        fromUser = message.fromUser,
        isError = message.isError,
        reasoningText = message.reasoningText,
        tools = message.tools.map { tool ->
            PersistedChatTool(
                id = tool.id,
                name = tool.name,
                summary = tool.summary,
                output = tool.output,
                isError = tool.isError,
            )
        },
        attachments = message.attachments.map { attachment ->
            PersistedChatAttachment(
                id = attachment.id,
                name = attachment.name,
                mimeType = attachment.mimeType,
                workspacePath = attachment.workspacePath,
                sizeBytes = attachment.sizeBytes,
            )
        },
        usage = message.usage?.let { usage ->
            PersistedChatUsage(
                inputTokens = usage.inputTokens,
                outputTokens = usage.outputTokens,
                totalTokens = usage.totalTokens,
                reasoningTokens = usage.reasoningTokens,
                cachedInputTokens = usage.cachedInputTokens,
            )
        },
        responseGroupId = message.responseGroupId,
        isActiveBranch = message.isActiveBranch,
        branchIndex = message.branchIndex,
    )
}

private fun PersistedChatMessage.toSharedChatMessage(): SharedChatMessage = SharedChatMessage(
    id = id,
    text = text,
    fromUser = fromUser,
    isError = isError,
    reasoningText = reasoningText,
    tools = tools.map { tool ->
        SharedChatToolInvocation(
            id = tool.id,
            name = tool.name,
            summary = tool.summary,
            output = tool.output,
            isRunning = false,
            isError = tool.isError,
        )
    },
    attachments = attachments.map { attachment ->
        SharedChatAttachment(
            id = attachment.id,
            name = attachment.name,
            mimeType = attachment.mimeType,
            workspacePath = attachment.workspacePath,
            sizeBytes = attachment.sizeBytes,
        )
    },
    usage = usage?.let { usage ->
        SharedPiUsage(
            inputTokens = usage.inputTokens,
            outputTokens = usage.outputTokens,
            totalTokens = usage.totalTokens,
            reasoningTokens = usage.reasoningTokens,
            cachedInputTokens = usage.cachedInputTokens,
        )
    },
    responseGroupId = responseGroupId,
    isActiveBranch = isActiveBranch,
    branchIndex = branchIndex,
)

@Composable
private fun SharedConversationDrawer(
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight().width(322.dp),
        drawerContainerColor = AetherSurface,
        drawerShape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = AetherOnSurface,
                    modifier = Modifier.weight(1f),
                )
                HeaderCircleButton(
                    icon = LucideIcons.SquarePen,
                    contentDescription = stringResource(Res.string.new_chat),
                    onClick = onNewChat,
                    size = 38.dp,
                    iconSize = 18.dp,
                    containerColor = AetherSurfaceHigh,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(CircleShape).background(AetherSurfaceHigh)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LucideIcons.Search, null, tint = AetherOnSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(Res.string.search_chats), color = AetherOnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(Res.string.today_label), style = MaterialTheme.typography.labelMedium, color = AetherOnSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.new_chat),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AetherSurfaceHigh)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                color = AetherOnSurface,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpenSettings)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LucideIcons.Settings, stringResource(Res.string.settings_title), tint = AetherOnSurface, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(Res.string.settings_title), color = AetherOnSurface, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SharedSettingsScreen(
    capabilities: PlatformCapabilities,
    runtime: MultiplatformLocalRuntime,
    platformServices: PlatformServices,
    providerConfig: LlmProviderConfig?,
    appSettings: AppSettings,
    usage: List<SharedPiUsage>,
    bridgeClient: SharedPiBridgeClient,
    skillManager: SharedSkillManager,
    installedSkills: List<SharedInstalledSkill>,
    onSkillsChanged: (List<SharedInstalledSkill>) -> Unit,
    mcpManager: SharedMcpManager,
    mcpServers: List<SharedMcpServerConfig>,
    onMcpServersChanged: (List<SharedMcpServerConfig>) -> Unit,
    chromeManager: SharedChromeManager,
    onProviderSaved: (LlmProviderConfig) -> Unit,
    onGeneralSettingsSaved: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit,
) {
    var destination by remember { mutableStateOf<SettingsDestination?>(null) }
    destination?.let { selected ->
        when (selected.kind) {
            SharedSettingsKind.General -> SharedGeneralSettingsDetail(
                settings = appSettings,
                onSave = onGeneralSettingsSaved,
                onBack = { destination = null },
            )
            SharedSettingsKind.Providers -> SharedProviderSettingsDetail(
                existingProviderConfig = providerConfig,
                bridgeClient = bridgeClient,
                onSave = onProviderSaved,
                onBack = { destination = null },
            )
            SharedSettingsKind.Personalization -> SharedPersonalizationSettingsDetail(
                settings = appSettings,
                onSave = onGeneralSettingsSaved,
                onBack = { destination = null },
            )
            SharedSettingsKind.WebTools -> SharedWebToolsSettingsDetail(
                settings = appSettings,
                onSave = onGeneralSettingsSaved,
                onBack = { destination = null },
            )
            SharedSettingsKind.Reliability -> SharedReliabilitySettingsDetail(
                settings = appSettings,
                capabilities = capabilities,
                onSave = onGeneralSettingsSaved,
                onBack = { destination = null },
            )
            SharedSettingsKind.Skills -> SharedSkillsSettingsDetail(
                skillManager = skillManager,
                runtime = runtime,
                platformServices = platformServices,
                installedSkills = installedSkills,
                onSkillsChanged = onSkillsChanged,
                onBack = { destination = null },
            )
            SharedSettingsKind.Mcp -> SharedMcpSettingsDetail(
                manager = mcpManager,
                servers = mcpServers,
                onServersChanged = onMcpServersChanged,
                onBack = { destination = null },
            )
            SharedSettingsKind.Alpine -> SharedAlpineSettingsDetail(
                chromeAvailable = capabilities.alpineChrome,
                onOpenTerminal = {
                    destination = SettingsDestination(
                        title = "Terminal",
                        subtitle = "Alpine shell",
                        kind = SharedSettingsKind.Terminal,
                    )
                },
                onOpenChrome = {
                    destination = SettingsDestination(
                        title = "Chrome",
                        subtitle = "Alpine Chromium",
                        kind = SharedSettingsKind.Chrome,
                    )
                },
                onBack = { destination = null },
            )
            SharedSettingsKind.Extensions -> SharedExtensionsSettingsDetail(
                bridgeClient = bridgeClient,
                capabilities = capabilities,
                onBack = { destination = null },
            )
            SharedSettingsKind.Terminal -> SharedTerminalScreen(
                runtime = runtime,
                onBack = {
                    destination = SettingsDestination(
                        title = "Alpine",
                        subtitle = "Ready for terminal, Node, and local tools",
                        kind = SharedSettingsKind.Alpine,
                    )
                },
            )
            SharedSettingsKind.Chrome -> SharedChromeScreen(
                manager = chromeManager,
                onBack = {
                    destination = SettingsDestination(
                        title = "Alpine",
                        subtitle = "Ready for terminal, Node, and local tools",
                        kind = SharedSettingsKind.Alpine,
                    )
                },
            )
            SharedSettingsKind.Statistics -> SharedStatisticsSettingsDetail(
                usage = usage,
                onBack = { destination = null },
            )
            SharedSettingsKind.Developer -> SharedDeveloperSettingsDetail(
                settings = appSettings,
                platformServices = platformServices,
                onSave = onGeneralSettingsSaved,
                onReplayOnboarding = onReplayOnboarding,
                onBack = { destination = null },
            )
            SharedSettingsKind.About -> SharedAboutSettingsDetail(
                platformServices = platformServices,
                onBack = { destination = null },
            )
            SharedSettingsKind.Generic -> SettingsDetail(
                selected = selected,
                onBack = { destination = null },
            )
        }
        return
    }

    fun open(title: String, subtitle: String) {
        destination = SettingsDestination(title, subtitle)
    }

    val general = SettingsDestination(
        stringResource(Res.string.general_title),
        stringResource(Res.string.general_subtitle),
        SharedSettingsKind.General,
    )
    val providers = SettingsDestination(
        stringResource(Res.string.providers_title),
        stringResource(Res.string.providers_subtitle),
        SharedSettingsKind.Providers,
    )
    val personalization = SettingsDestination(
        stringResource(Res.string.personalization_title),
        stringResource(Res.string.personalization_subtitle),
        SharedSettingsKind.Personalization,
    )
    val webTools = SettingsDestination(
        stringResource(Res.string.web_tools_title),
        stringResource(Res.string.web_tools_subtitle),
        SharedSettingsKind.WebTools,
    )
    val reliability = SettingsDestination(
        stringResource(Res.string.reliability_title),
        stringResource(Res.string.reliability_subtitle),
        SharedSettingsKind.Reliability,
    )
    val skills = SettingsDestination(
        stringResource(Res.string.skills_title),
        stringResource(Res.string.skills_subtitle),
        SharedSettingsKind.Skills,
    )
    val extensions = SettingsDestination(
        stringResource(Res.string.extensions_title),
        stringResource(Res.string.extensions_subtitle),
        SharedSettingsKind.Extensions,
    )
    val mcp = SettingsDestination(
        stringResource(Res.string.mcp_title),
        stringResource(Res.string.mcp_subtitle),
        SharedSettingsKind.Mcp,
    )
    val alpine = SettingsDestination(
        stringResource(Res.string.alpine_runtime_title),
        stringResource(Res.string.alpine_subtitle),
        SharedSettingsKind.Alpine,
    )
    val statistics = SettingsDestination(
        stringResource(Res.string.statistics_title),
        stringResource(Res.string.statistics_subtitle),
        SharedSettingsKind.Statistics,
    )
    val developer = SettingsDestination(
        stringResource(Res.string.developer_title),
        stringResource(Res.string.developer_subtitle),
        SharedSettingsKind.Developer,
    )
    val about = SettingsDestination(
        stringResource(Res.string.about_title),
        stringResource(Res.string.about_subtitle),
        SharedSettingsKind.About,
    )
    val extensionUiController = LocalSharedAetherExtensionUiController.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AetherBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(top = 92.dp, start = 20.dp, end = 20.dp).imePadding().navigationBarsPadding(),
            ) {
                SharedAetherExtensionSlot(
                    SharedExtensionSlotSettingsHub,
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
                val extensionPages = extensionUiController?.snapshot?.pages.orEmpty()
                if (extensionPages.isNotEmpty()) {
                    SettingsCardGroup {
                        extensionPages.forEachIndexed { index, page ->
                            SharedAetherExtensionPageLauncher(
                                page = page,
                                onClick = {
                                    extensionUiController?.onOpenPage?.invoke(page.id)
                                },
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = if (index == 0) 8.dp else 0.dp,
                                    bottom = 8.dp,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                SettingsCardGroup {
                    SettingsNavRow(Icons.Rounded.AutoAwesome, general.title, general.subtitle) {
                        destination = general
                    }
                }
                Spacer(Modifier.height(16.dp))
                SettingsCardGroup {
                    SettingsNavRow(Icons.Rounded.Cloud, providers.title, providers.subtitle) {
                        destination = providers
                    }
                    CardDivider()
                    SettingsNavRow(Icons.Rounded.Person, personalization.title, personalization.subtitle) {
                        destination = personalization
                    }
                    CardDivider()
                    SettingsNavRow(Icons.Rounded.Link, webTools.title, webTools.subtitle) {
                        destination = webTools
                    }
                    CardDivider()
                    SettingsNavRow(Icons.Rounded.Refresh, reliability.title, reliability.subtitle) {
                        destination = reliability
                    }
                }
                Spacer(Modifier.height(16.dp))
                SettingsCardGroup {
                    SettingsNavRow(Icons.Rounded.Extension, skills.title, skills.subtitle) {
                        destination = skills
                    }
                    CardDivider()
                    SettingsNavRow(Icons.Rounded.Code, extensions.title, extensions.subtitle) {
                        destination = extensions
                    }
                    CardDivider()
                    SettingsNavRow(Icons.Rounded.Code, mcp.title, mcp.subtitle) {
                        destination = mcp
                    }
                    if (capabilities.scheduledTasks) {
                        CardDivider()
                        SettingsNavRow(Icons.Rounded.Schedule, "Scheduled Tasks", "Run saved tasks on a schedule") { }
                    }
                    CardDivider()
                    SettingsNavRow(Icons.Rounded.Terminal, alpine.title, alpine.subtitle) {
                        destination = alpine
                    }
                    if (capabilities.termux) {
                        CardDivider()
                        SettingsNavRow(Icons.Rounded.Terminal, "Termux", "Android terminal integration") { }
                    }
                    if (capabilities.runtimeSelection) {
                        CardDivider()
                        SettingsNavRow(Icons.Rounded.Check, "Runtime defaults", "Choose the default runtime") { }
                    }
                    if (capabilities.agentMode) {
                        CardDivider()
                        SettingsNavRow(LucideIcons.MousePointer2, "Agent Mode", "Control the Android device") { }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SettingsCardGroup {
                    SettingsNavRow(Icons.Rounded.BarChart, statistics.title, statistics.subtitle) {
                        destination = statistics
                    }
                }
                Spacer(Modifier.height(16.dp))
                SettingsCardGroup {
                    SettingsNavRow(Icons.Rounded.AutoAwesome, stringResource(Res.string.tour_title), stringResource(Res.string.tour_subtitle), onClick = onReplayOnboarding)
                    CardDivider()
                    SettingsNavRow(Icons.Rounded.Code, developer.title, developer.subtitle) {
                        destination = developer
                    }
                }
                Spacer(Modifier.height(16.dp))
                SettingsCardGroup {
                    SettingsNavRow(Icons.Rounded.Info, about.title, about.subtitle) {
                        destination = about
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
            SettingsTopBar(title = stringResource(Res.string.settings_title), onBack = onBack)
        }
    }
}

private data class SharedExtensionPackage(
    val source: String,
    val name: String,
    val version: String,
    val description: String,
    val nativeEntrypointCount: Int,
)

@Composable
private fun SharedExtensionsSettingsDetail(
    bridgeClient: SharedPiBridgeClient,
    capabilities: PlatformCapabilities,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var packages by remember { mutableStateOf<List<SharedExtensionPackage>>(emptyList()) }
    var source by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun applyPackages(payload: JsonObject) {
        packages = (payload["packages"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.map { item ->
                SharedExtensionPackage(
                    source = item.string("source"),
                    name = item.string("name").ifBlank { item.string("source") },
                    version = item.string("version"),
                    description = item.string("description"),
                    nativeEntrypointCount = item["native_entrypoint_count"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.toIntOrNull()
                        ?: 0,
                )
            }
            .orEmpty()
    }

    fun runOperation(operation: suspend () -> JsonObject) {
        if (busy) return
        busy = true
        status = ""
        scope.launch {
            runCatching { operation() }.fold(
                onSuccess = {
                    applyPackages(it)
                    status = "Extensions refreshed."
                },
                onFailure = { status = it.message.orEmpty() },
            )
            busy = false
        }
    }

    LaunchedEffect(bridgeClient) {
        busy = true
        runCatching { bridgeClient.listExtensionPackages() }
            .onSuccess(::applyPackages)
            .onFailure { status = it.message.orEmpty() }
        busy = false
    }

    Box(modifier = Modifier.fillMaxSize().background(AetherBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 98.dp, start = 20.dp, end = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsCardGroup {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    BasicTextField(
                        value = source,
                        onValueChange = { source = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = AetherOnSurface),
                        cursorBrush = SolidColor(AetherOnSurface),
                        singleLine = true,
                        decorationBox = { field ->
                            Box(modifier = Modifier.padding(vertical = 8.dp)) {
                                if (source.isBlank()) {
                                    Text("npm:package-name", color = AetherOnSurfaceVariant)
                                }
                                field()
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val value = source.trim()
                            if (value.isNotBlank()) {
                                runOperation { bridgeClient.installExtensionPackage(value) }
                                source = ""
                            }
                        },
                        enabled = source.isNotBlank() && !busy,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AetherOnSurface,
                            contentColor = AetherBackground,
                        ),
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Install package")
                    }
                }
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodySmall, color = AetherOnSurfaceVariant)
            }
            packages.forEach { extension ->
                SettingsCardGroup {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(extension.name, style = MaterialTheme.typography.titleMedium, color = AetherOnSurface)
                        if (extension.version.isNotBlank()) {
                            Text(extension.version, style = MaterialTheme.typography.labelMedium, color = AetherOnSurfaceVariant)
                        }
                        if (extension.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(extension.description, style = MaterialTheme.typography.bodyMedium, color = AetherOnSurfaceVariant)
                        }
                        if (extension.nativeEntrypointCount > 0 && !capabilities.nativeMods) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Native Mod components are available on Android only. Script components remain enabled.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AetherOnSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { runOperation { bridgeClient.updateExtensionPackage(extension.source) } },
                                enabled = !busy,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = AetherSurfaceHigher),
                            ) {
                                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Update", color = AetherOnSurface)
                            }
                            Button(
                                onClick = { runOperation { bridgeClient.removeExtensionPackage(extension.source) } },
                                enabled = !busy,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = AetherSurfaceHigher),
                            ) {
                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        SettingsTopBar(title = stringResource(Res.string.extensions_title), onBack = onBack)
    }
}

@Composable
private fun SharedAlpineSettingsDetail(
    chromeAvailable: Boolean,
    onOpenTerminal: () -> Unit,
    onOpenChrome: () -> Unit,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(AetherBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 98.dp, start = 20.dp, end = 20.dp)
                .navigationBarsPadding(),
        ) {
            SettingsCardGroup {
                SettingsNavRow(
                    icon = Icons.Rounded.Terminal,
                    title = stringResource(Res.string.settings_open_terminal),
                    subtitle = stringResource(Res.string.alpine_subtitle),
                    onClick = onOpenTerminal,
                )
                if (chromeAvailable) {
                    CardDivider()
                    SettingsNavRow(
                        icon = Icons.Rounded.Public,
                        title = stringResource(Res.string.settings_open_chrome),
                        subtitle = stringResource(Res.string.settings_chrome_environment),
                        onClick = onOpenChrome,
                    )
                }
            }
        }
        SettingsTopBar(title = stringResource(Res.string.alpine_runtime_title), onBack = onBack)
    }
}

@Composable
private fun SharedProviderSettingsDetail(
    existingProviderConfig: LlmProviderConfig?,
    bridgeClient: SharedPiBridgeClient,
    onSave: (LlmProviderConfig) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val formState = rememberProviderFormState(existingProviderConfig)
    val modelCatalogClient = remember { SharedProviderModelCatalogClient() }
    var authState by remember { mutableStateOf(PiProviderAuthState()) }
    var fetchingModels by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().background(AetherBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 94.dp, start = 20.dp, end = 20.dp)
                .navigationBarsPadding(),
        ) {
            AddProviderWizard(
                state = formState,
                existingProviderIds = emptySet(),
                isFetchingModels = fetchingModels,
                onFetchModels = { config, callback ->
                    fetchingModels = true
                    scope.launch {
                        val models = runCatching {
                            modelCatalogClient.fetchModels(config, bridgeClient::listProviders).models
                        }.getOrDefault(emptyList())
                            .ifEmpty {
                                listOf(PiProviderCatalog.resolve(config.piProviderId).defaultModelId)
                                    .filter(String::isNotBlank)
                            }
                        fetchingModels = false
                        callback(models)
                    }
                },
                authState = authState,
                onStartProviderLogin = { configId, providerId, authMethod, oauthFlow ->
                    authState = PiProviderAuthState(
                        providerId = providerId,
                        authMethod = authMethod,
                        isRunning = true,
                        statusMessage = "Waiting for authorization.",
                    )
                    scope.launch {
                        runCatching {
                            bridgeClient.loginProvider(
                                providerConfigId = configId,
                                providerId = providerId,
                                authMethod = authMethod.storageValue,
                                oauthFlow = oauthFlow,
                            ) { event, payload ->
                                authState = authState.withBridgeAuthEvent(event, payload)
                            }
                        }.fold(
                            onSuccess = { payload ->
                                authState = authState.copy(
                                    isRunning = false,
                                    prompt = null,
                                    apiKey = payload.string("api_key"),
                                    oauthCredentialJson = (payload["oauth_credential"] as? JsonObject)
                                        ?.toString()
                                        .orEmpty(),
                                    providerEnvironmentVariables = payload.toPiProviderEnvironmentVariables(),
                                    statusMessage = "Provider connected.",
                                    errorMessage = "",
                                )
                            },
                            onFailure = { error ->
                                authState = authState.copy(
                                    isRunning = false,
                                    prompt = null,
                                    errorMessage = error.message.orEmpty(),
                                )
                            },
                        )
                    }
                },
                onSubmitAuthPrompt = { promptId, value, cancelled ->
                    scope.launch {
                        runCatching { bridgeClient.submitAuthPrompt(promptId, value, cancelled) }
                        authState = authState.copy(prompt = null)
                    }
                },
                onClearAuthState = { authState = PiProviderAuthState() },
                onSave = { config ->
                    onSave(config)
                    onBack()
                },
            )
        }
        SettingsTopBar(title = stringResource(Res.string.providers_title), onBack = onBack)
    }
}

@Composable
private fun SettingsDetail(selected: SettingsDestination, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(AetherBackground)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 98.dp, start = 20.dp, end = 20.dp).navigationBarsPadding(),
        ) {
            SettingsCardGroup {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(selected.title, style = MaterialTheme.typography.titleMedium, color = AetherOnSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(selected.subtitle, style = MaterialTheme.typography.bodyMedium, color = AetherOnSurfaceVariant)
                }
            }
        }
        SettingsTopBar(title = selected.title, onBack = onBack)
    }
}

@Composable
internal fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(AetherBackground).statusBarsPadding()
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCircleButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(Res.string.back_label),
                onClick = onBack,
                size = 38.dp,
                iconSize = 19.dp,
                containerColor = AetherSurface,
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = AetherOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
            )
            Spacer(Modifier.size(38.dp))
        }
        Spacer(
            modifier = Modifier.fillMaxWidth().height(TopFadeHeight).background(
                Brush.verticalGradient(listOf(AetherBackground, Color.Transparent))
            )
        )
    }
}
