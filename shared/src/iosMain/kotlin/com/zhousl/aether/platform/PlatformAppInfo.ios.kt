package com.zhousl.aether.platform

import com.zhousl.aether.data.AppLanguage
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

actual fun platformAppVersion(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        ?: "1.0"

actual fun applyPlatformAppLanguage(language: AppLanguage) {
    NSUserDefaults.standardUserDefaults.setObject(
        listOf(language.languageTag),
        forKey = "AppleLanguages",
    )
    NSUserDefaults.standardUserDefaults.synchronize()
}

