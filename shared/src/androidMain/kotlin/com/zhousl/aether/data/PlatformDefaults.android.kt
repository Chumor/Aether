package com.zhousl.aether.data

import java.util.Locale
import java.util.UUID

actual fun platformCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun platformRandomUuid(): String = UUID.randomUUID().toString()

actual fun platformLanguageTag(): String = Locale.getDefault().toLanguageTag()

actual fun platformDefaultSystemPrompt(): String =
    "You are Aether, a local-first Android agent that can call tools and complete tasks on-device. Use available tools instead of guessing local state."

actual fun platformDefaultLlmUserAgent(): String = "Aether/1.0 (Android)"

fun defaultAppLanguage(locale: Locale): AppLanguage = if (
    locale.language.equals("zh", ignoreCase = true)
) {
    AppLanguage.SimplifiedChinese
} else {
    AppLanguage.English
}
