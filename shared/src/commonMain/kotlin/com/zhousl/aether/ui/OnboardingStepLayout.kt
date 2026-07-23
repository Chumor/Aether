package com.zhousl.aether.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhousl.aether.shared.resources.Res
import com.zhousl.aether.shared.resources.back_label
import com.zhousl.aether.ui.theme.AetherBackground
import com.zhousl.aether.ui.theme.AetherOnSurface
import com.zhousl.aether.ui.theme.AetherOnSurfaceVariant
import com.zhousl.aether.ui.theme.AetherOutlineSoft
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val SharedMessageTravelDuration = 1_520
private const val SharedContentFadeDuration = 920
private const val SharedMessageSettleDelayMillis = 800L
private const val SharedMessageMinDurationMillis = 1_000L
private const val SharedMessageMaxDurationMillis = 3_300L
private val SharedTourEasing = CubicBezierEasing(0.22f, 0.84f, 0.18f, 1f)

@Composable
fun OnboardingConversationStepPage(
    stepIndex: Int,
    stepCount: Int,
    message: String,
    onBack: (() -> Unit)?,
    topRightLabel: String,
    onTopRight: () -> Unit,
    isExiting: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pageKey = remember(stepIndex, stepCount, message) { "$stepIndex/$stepCount:$message" }
    val contentVisible = rememberSharedStepContentVisible(pageKey, message)
    val topPadding by animateDpAsState(
        targetValue = if (contentVisible) 56.dp else 168.dp,
        animationSpec = tween(
            durationMillis = SharedMessageTravelDuration,
            easing = SharedTourEasing,
        ),
        label = "tour_message_travel",
    )

    Box(modifier = Modifier.fillMaxSize().background(AetherBackground)) {
        AnimatedVisibility(
            visible = !isExiting,
            enter = fadeIn(animationSpec = tween(durationMillis = 0)),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 280, easing = SharedTourEasing),
            ),
            label = "step_page_visibility",
        ) {
            Column(
                modifier = Modifier.fillMaxSize().imePadding().navigationBarsPadding(),
            ) {
                SharedTourChromeBar(
                    stepIndex = stepIndex,
                    stepCount = stepCount,
                    onBack = onBack,
                    topRightLabel = topRightLabel,
                    onTopRight = onTopRight,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 28.dp, top = 12.dp, end = 28.dp, bottom = 20.dp),
                ) {
                    Spacer(modifier = Modifier.height(topPadding))
                    SharedStreamingStepMessage(playKey = pageKey, text = message)
                    Spacer(modifier = Modifier.height(32.dp))
                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = SharedContentFadeDuration,
                                delayMillis = 180,
                                easing = SharedTourEasing,
                            ),
                        ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 180)),
                        label = "step_content_fade",
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            content = content,
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
fun OnboardingStepLead(
    icon: ImageVector,
    accent: Color,
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AetherOnSurface,
            )
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = AetherOnSurfaceVariant,
        )
    }
}

@Composable
fun OnboardingPrimaryActionButton(
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    onClick: () -> Unit,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White,
            disabledContainerColor = AetherOutlineSoft,
            disabledContentColor = AetherOnSurfaceVariant,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 1.8.dp,
                color = Color.White,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(label)
    }
}

@Composable
fun OnboardingActionRow(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingPrimaryActionButton(
            label = primaryLabel,
            modifier = Modifier.weight(1f),
            enabled = primaryEnabled,
            isLoading = primaryLoading,
            onClick = onPrimary,
        )
        TextButton(onClick = onSecondary, modifier = Modifier.weight(0.62f)) {
            Text(
                text = secondaryLabel,
                color = AetherOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SharedTourChromeBar(
    stepIndex: Int,
    stepCount: Int,
    onBack: (() -> Unit)?,
    topRightLabel: String,
    onTopRight: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AetherBackground)
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(Res.string.back_label),
                        tint = AetherOnSurface,
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(stepCount) { index ->
                    Box(
                        modifier = Modifier
                            .width(if (index + 1 == stepIndex) 20.dp else 7.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (index + 1 == stepIndex) AetherOnSurface else AetherOutlineSoft,
                            ),
                    )
                }
            }
            TextButton(onClick = onTopRight) {
                Text(text = topRightLabel, color = AetherOnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun rememberSharedStepContentVisible(key: Any, message: String): Boolean {
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key, message) {
        visible = false
        delay(sharedMessageRevealDuration(message) + SharedMessageSettleDelayMillis)
        visible = true
    }
    return visible
}

@Composable
private fun SharedStreamingStepMessage(playKey: Any, text: String) {
    var revealed by remember(playKey, text) { mutableStateOf("") }
    LaunchedEffect(playKey, text) {
        revealed = ""
        splitSharedRevealUnits(text).forEach { unit ->
            delay(sharedRevealUnitDelay(unit))
            revealed += unit
        }
    }
    Text(
        text = revealed.ifEmpty { " " },
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
        color = AetherOnSurface,
    )
}

private fun splitSharedRevealUnits(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    val units = mutableListOf<String>()
    val builder = StringBuilder()
    text.forEach { char ->
        builder.append(char)
        if (char == ' ' || char == '\n' || char == '.' || char == '!' || char == '?' || char == ',') {
            units += builder.toString()
            builder.clear()
        }
    }
    if (builder.isNotEmpty()) units += builder.toString()
    return units
}

private fun sharedRevealUnitDelay(unit: String): Long {
    val trimmed = unit.trim()
    if (trimmed.isEmpty()) return 18L
    if (trimmed.length == 1 && trimmed.first() in setOf('.', ',', '!', '?')) return 180L
    return (80L + trimmed.length * 18L).coerceIn(96L, 240L)
}

private fun sharedMessageRevealDuration(message: String): Long =
    splitSharedRevealUnits(message)
        .sumOf(::sharedRevealUnitDelay)
        .coerceIn(SharedMessageMinDurationMillis, SharedMessageMaxDurationMillis)
