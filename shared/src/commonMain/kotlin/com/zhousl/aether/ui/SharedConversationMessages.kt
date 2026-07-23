package com.zhousl.aether.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.zhousl.aether.data.pi.SharedPiUsage
import com.zhousl.aether.ui.theme.AetherOnSurface
import com.zhousl.aether.ui.theme.AetherOnSurfaceVariant
import com.zhousl.aether.ui.theme.AetherSurfaceHigh
import com.zhousl.aether.ui.theme.AetherSurfaceHigher

internal data class SharedChatToolInvocation(
    val id: String,
    val name: String,
    val summary: String,
    val output: String = "",
    val isRunning: Boolean = true,
    val isError: Boolean = false,
)

internal data class SharedChatAttachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val workspacePath: String,
    val sizeBytes: Long = 0,
)

@Composable
internal fun SharedPendingInputBubble(pending: SharedPendingTurn) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(22.dp))
                .background(AetherSurfaceHigh).padding(horizontal = 16.dp, vertical = 11.dp),
        ) {
            Text("Queued", color = AetherOnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            if (pending.text.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(pending.text, color = AetherOnSurface, style = MaterialTheme.typography.bodyLarge)
            }
            if (pending.attachments.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${pending.attachments.size} attachment${if (pending.attachments.size == 1) "" else "s"}",
                    color = AetherOnSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun SharedConversationMessage(
    message: SharedChatMessage,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onCopy: (String) -> Boolean,
    onShare: (String) -> Boolean,
    onEdit: () -> Unit,
    onPreviousBranch: () -> Unit,
    onNextBranch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        if (message.fromUser) {
            var menuOpen by remember(message.id) { mutableStateOf(false) }
            Box {
                Column(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AetherOnSurface,
                        )
                    }
                    message.attachments.forEachIndexed { index, attachment ->
                        if (message.text.isNotBlank() || index > 0) Spacer(Modifier.height(7.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Code, null, tint = AetherOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(
                                attachment.name,
                                color = AetherOnSurface,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = {
                            menuOpen = false
                            onCopy(message.text)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit and resend") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                if (message.reasoningText.isNotBlank()) {
                    SharedReasoningStatus(
                        reasoning = message.reasoningText,
                        isRunning = message.isStreaming && message.text.isBlank(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                message.tools.forEach { tool ->
                    SharedToolInvocationCard(tool)
                    Spacer(Modifier.height(8.dp))
                }
                if (message.text.isNotBlank()) {
                    if (message.isError) {
                        Text(
                            text = message.text,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Markdown(content = message.text, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (message.isStreaming && message.text.isBlank() && message.reasoningText.isBlank() && message.tools.isEmpty()) {
                    SharedThinkingIndicator(message.status)
                }
                if (!message.isStreaming && message.text.isNotBlank()) {
                    SharedMessageActions(
                        text = message.text,
                        usage = message.usage,
                        canRetry = canRetry,
                        onCopy = onCopy,
                        onShare = onShare,
                        onRetry = onRetry,
                        branchIndex = message.branchIndex,
                        branchCount = message.branchCount,
                        onPreviousBranch = onPreviousBranch,
                        onNextBranch = onNextBranch,
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedReasoningStatus(reasoning: String, isRunning: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AetherSurfaceHigh)
            .clickable { expanded = !expanded }
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 1.6.dp,
                    color = AetherOnSurfaceVariant,
                )
            } else {
                Icon(Icons.Rounded.Check, null, tint = AetherOnSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = if (isRunning) "Thinking" else "Thought process",
                modifier = Modifier.weight(1f),
                color = AetherOnSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Icon(Icons.Rounded.ExpandMore, null, tint = AetherOnSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        AnimatedVisibility(expanded) {
            Text(
                text = reasoning,
                modifier = Modifier.padding(top = 10.dp),
                color = AetherOnSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SharedToolInvocationCard(tool: SharedChatToolInvocation) {
    var expanded by remember(tool.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AetherSurfaceHigh)
            .clickable(enabled = tool.output.isNotBlank()) { expanded = !expanded }
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(AetherSurfaceHigher),
                contentAlignment = Alignment.Center,
            ) {
                if (tool.isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 1.6.dp)
                } else {
                    Icon(
                        if (tool.isError) Icons.Rounded.Close else Icons.Rounded.Code,
                        null,
                        tint = if (tool.isError) MaterialTheme.colorScheme.error else AetherOnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = toolTitle(tool.name),
                    color = AetherOnSurface,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (tool.summary.isNotBlank()) {
                    Text(
                        text = tool.summary,
                        color = AetherOnSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        AnimatedVisibility(expanded && tool.output.isNotBlank()) {
            Text(
                text = tool.output,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                color = AetherOnSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 24,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SharedThinkingIndicator(status: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 1.7.dp,
            color = AetherOnSurfaceVariant,
        )
        Spacer(Modifier.width(9.dp))
        Text(status.ifBlank { "Thinking" }, color = AetherOnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SharedMessageActions(
    text: String,
    usage: SharedPiUsage?,
    canRetry: Boolean,
    onCopy: (String) -> Boolean,
    onShare: (String) -> Boolean,
    onRetry: () -> Unit,
    branchIndex: Int,
    branchCount: Int,
    onPreviousBranch: () -> Unit,
    onNextBranch: () -> Unit,
) {
    var showStatistics by remember { mutableStateOf(false) }
    Column {
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MessageActionIcon(Icons.Rounded.ContentCopy, "Copy", enabled = true) { onCopy(text) }
            MessageActionIcon(Icons.Rounded.Share, "Share", enabled = true) { onShare(text) }
            if (canRetry) MessageActionIcon(Icons.Rounded.Refresh, "Retry", enabled = true, onClick = onRetry)
            if (branchCount > 1) {
                MessageActionIcon(
                    Icons.Rounded.ChevronLeft,
                    "Previous response",
                    enabled = branchIndex > 0,
                    onClick = onPreviousBranch,
                )
                Text(
                    text = "${branchIndex + 1}/$branchCount",
                    color = AetherOnSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 10.dp),
                )
                MessageActionIcon(
                    Icons.Rounded.ChevronRight,
                    "Next response",
                    enabled = branchIndex < branchCount - 1,
                    onClick = onNextBranch,
                )
            }
            if (usage != null) {
                MessageActionIcon(Icons.Rounded.Info, "Statistics", enabled = true) {
                    showStatistics = !showStatistics
                }
            }
        }
        AnimatedVisibility(showStatistics && usage != null) {
            if (usage != null) SharedUsageStatisticsPanel(usage)
        }
    }
}

@Composable
private fun SharedUsageStatisticsPanel(usage: SharedPiUsage) {
    Column(
        modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp))
            .background(AetherSurfaceHigh).padding(horizontal = 13.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("${usage.totalTokens} tokens", color = AetherOnSurface, style = MaterialTheme.typography.labelLarge)
        Text(
            "Input ${usage.inputTokens}  Output ${usage.outputTokens}  Reasoning ${usage.reasoningTokens}  Cached ${usage.cachedInputTokens}",
            color = AetherOnSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MessageActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) AetherOnSurfaceVariant else Color.Transparent,
            modifier = Modifier.size(17.dp),
        )
    }
}

private fun toolTitle(name: String): String = when (name) {
    "read" -> "Read file"
    "write" -> "Write file"
    "edit" -> "Edit file"
    "ls" -> "List files"
    "find" -> "Find files"
    "grep" -> "Search files"
    "bash" -> "Run command"
    "sleep" -> "Wait"
    "web_fetch" -> "Fetch web page"
    "web_search" -> "Search the web"
    "chrome" -> "Use Chrome"
    else -> if (name.startsWith("mcp__")) {
        name.removePrefix("mcp__").replace("__", " / ").replace('_', ' ')
    } else name.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
