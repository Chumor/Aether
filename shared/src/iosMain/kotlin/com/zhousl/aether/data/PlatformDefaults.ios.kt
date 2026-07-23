package com.zhousl.aether.data

import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.NSUUID
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.timeIntervalSince1970

actual fun platformCurrentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1_000.0).toLong()

actual fun platformRandomUuid(): String = NSUUID().UUIDString()

actual fun platformLanguageTag(): String = NSLocale.currentLocale.languageCode

actual fun platformDefaultSystemPrompt(): String =
    "You are Aether, a local-first agent that can call tools and complete tasks on-device. Use available tools instead of guessing local state."

actual fun platformDefaultLlmUserAgent(): String = "Aether/1.0 (iOS)"
