package com.zhousl.aether.data.chatdb

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.zhousl.aether.data.platformCurrentTimeMillis

data class PersistedChatMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    val isError: Boolean = false,
    val reasoningText: String = "",
    val tools: List<PersistedChatTool> = emptyList(),
    val attachments: List<PersistedChatAttachment> = emptyList(),
    val usage: PersistedChatUsage? = null,
    val responseGroupId: String = "",
    val isActiveBranch: Boolean = true,
    val branchIndex: Int = 0,
)

data class PersistedChatUsage(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val totalTokens: Long = 0,
    val reasoningTokens: Long = 0,
    val cachedInputTokens: Long = 0,
)

data class PersistedChatTool(
    val id: String,
    val name: String,
    val summary: String,
    val output: String = "",
    val isError: Boolean = false,
)

data class PersistedChatAttachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val workspacePath: String,
    val sizeBytes: Long = 0,
)

data class PersistedChatSession(
    val id: String,
    val title: String,
    val preview: String,
    val messages: List<PersistedChatMessage>,
    val selectedSkillIds: List<String> = emptyList(),
    val activeMcpServerIds: List<String> = emptyList(),
    val chromeEnabled: Boolean = false,
    val selectedModelKey: String = "",
)

class SharedChatHistoryStore(
    database: ChatHistoryDatabase,
) {
    private val dao = database.chatHistoryDao()

    suspend fun loadMostRecent(): PersistedChatSession? {
        val session = dao.getSessions().firstOrNull() ?: return null
        return load(session.id)
    }

    suspend fun loadAll(): List<PersistedChatSession> =
        dao.getSessions().mapNotNull { session -> load(session.id) }

    suspend fun load(sessionId: String): PersistedChatSession? {
        val session = dao.getSession(sessionId) ?: return null
        val messages = dao.getMessagesForSession(session.id).mapNotNull { entity ->
            runCatching {
                val json = Json.parseToJsonElement(entity.messageJson).jsonObject
                PersistedChatMessage(
                    id = json.string("id").ifBlank { entity.id },
                    text = json.string("text"),
                    fromUser = json["fromUser"]?.jsonPrimitive?.booleanOrNull ?: false,
                    isError = json["isError"]?.jsonPrimitive?.booleanOrNull ?: false,
                    reasoningText = json.string("reasoningText"),
                    responseGroupId = json.string("responseGroupId")
                        .ifBlank { entity.responseGroupId.orEmpty() },
                    isActiveBranch = json["isActiveBranch"]?.jsonPrimitive?.booleanOrNull ?: true,
                    branchIndex = json["branchIndex"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                    tools = (json["tools"] as? JsonArray).orEmpty().mapNotNull { element ->
                        val tool = element as? JsonObject ?: return@mapNotNull null
                        PersistedChatTool(
                            id = tool.string("id"),
                            name = tool.string("name"),
                            summary = tool.string("summary"),
                            output = tool.string("output"),
                            isError = tool["isError"]?.jsonPrimitive?.booleanOrNull ?: false,
                        )
                    },
                    attachments = (json["attachments"] as? JsonArray).orEmpty().mapNotNull { element ->
                        val attachment = element as? JsonObject ?: return@mapNotNull null
                        PersistedChatAttachment(
                            id = attachment.string("id"),
                            name = attachment.string("name"),
                            mimeType = attachment.string("mimeType"),
                            workspacePath = attachment.string("workspacePath"),
                            sizeBytes = attachment["sizeBytes"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                        )
                    },
                    usage = (json["usage"] as? JsonObject)?.let { usage ->
                        PersistedChatUsage(
                            inputTokens = usage.long("inputTokens"),
                            outputTokens = usage.long("outputTokens"),
                            totalTokens = usage.long("totalTokens"),
                            reasoningTokens = usage.long("reasoningTokens"),
                            cachedInputTokens = usage.long("cachedInputTokens"),
                        )
                    },
                )
            }.getOrNull()
        }
        return PersistedChatSession(
            id = session.id,
            title = session.title,
            preview = session.preview,
            messages = messages,
            selectedSkillIds = parseStringArray(session.selectedSkillIdsJson),
            activeMcpServerIds = parseStringArray(session.activeMcpServerIdsJson),
            chromeEnabled = session.chromeEnabled,
            selectedModelKey = session.selectedModelKey,
        )
    }

    suspend fun rename(sessionId: String, title: String) {
        val session = dao.getSession(sessionId) ?: return
        dao.upsertSession(session.copy(title = title.trim(), hasCustomTitle = true))
    }

    suspend fun delete(sessionId: String) {
        dao.deleteSession(sessionId)
    }

    suspend fun save(
        sessionId: String,
        messages: List<PersistedChatMessage>,
        selectedSkillIds: List<String> = emptyList(),
        activeMcpServerIds: List<String> = emptyList(),
        chromeEnabled: Boolean = false,
        selectedModelKey: String = "",
    ) {
        val preview = messages.lastOrNull()?.text.orEmpty().take(160)
        dao.upsertSession(
            ChatSessionEntity(
                id = sessionId,
                title = messages.firstOrNull { it.fromUser }?.text.orEmpty().take(80).ifBlank { "New chat" },
                preview = preview,
                hasCustomTitle = false,
                selectedSkillIdsJson = buildJsonArray {
                    selectedSkillIds.distinct().forEach { add(JsonPrimitive(it)) }
                }.toString(),
                activeSkillsJson = "[]",
                activeMcpServerIdsJson = buildJsonArray {
                    activeMcpServerIds.distinct().forEach { add(JsonPrimitive(it)) }
                }.toString(),
                agentModeEnabled = false,
                chromeEnabled = chromeEnabled,
                selectedModelKey = selectedModelKey,
                sortOrder = -platformSortOrder(),
            )
        )
        dao.deleteMessagesForSession(sessionId)
        dao.upsertMessages(
            messages.mapIndexed { index, message ->
                val json = buildJsonObject {
                    put("id", message.id)
                    put("text", message.text)
                    put("fromUser", message.fromUser)
                    put("isError", message.isError)
                    put("reasoningText", message.reasoningText)
                    put("responseGroupId", message.responseGroupId)
                    put("isActiveBranch", message.isActiveBranch)
                    put("branchIndex", message.branchIndex)
                    put("tools", buildJsonArray {
                        message.tools.forEach { tool ->
                            add(buildJsonObject {
                                put("id", tool.id)
                                put("name", tool.name)
                                put("summary", tool.summary)
                                put("output", tool.output)
                                put("isError", tool.isError)
                            })
                        }
                    })
                    put("attachments", buildJsonArray {
                        message.attachments.forEach { attachment ->
                            add(buildJsonObject {
                                put("id", attachment.id)
                                put("name", attachment.name)
                                put("mimeType", attachment.mimeType)
                                put("workspacePath", attachment.workspacePath)
                                put("sizeBytes", attachment.sizeBytes)
                            })
                        }
                    })
                    message.usage?.let { usage ->
                        put("usage", buildJsonObject {
                            put("inputTokens", usage.inputTokens)
                            put("outputTokens", usage.outputTokens)
                            put("totalTokens", usage.totalTokens)
                            put("reasoningTokens", usage.reasoningTokens)
                            put("cachedInputTokens", usage.cachedInputTokens)
                        })
                    }
                }
                ChatMessageEntity(
                    sessionId = sessionId,
                    id = message.id,
                    position = index,
                    messageJson = json.toString(),
                    author = if (message.fromUser) "User" else "Agent",
                    text = message.text,
                    createdAtMillis = platformSortOrder(),
                    responseGroupId = message.responseGroupId.ifBlank { null },
                )
            }
        )
        dao.upsertMeta(
            ChatStateMetaEntity(
                currentSessionId = sessionId,
                roomMigrationComplete = true,
                workspaceFileRefsComplete = true,
            )
        )
    }
}

private fun platformSortOrder(): Long = platformCurrentTimeMillis()

private fun parseStringArray(value: String): List<String> = runCatching {
    (Json.parseToJsonElement(value) as? JsonArray)
        .orEmpty()
        .mapNotNull { it.jsonPrimitive.contentOrNull }
        .filter(String::isNotBlank)
        .distinct()
}.getOrDefault(emptyList())

private fun kotlinx.serialization.json.JsonObject.string(name: String): String =
    get(name)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun kotlinx.serialization.json.JsonObject.long(name: String): Long =
    get(name)?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0
