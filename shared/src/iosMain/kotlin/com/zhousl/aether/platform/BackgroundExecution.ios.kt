package com.zhousl.aether.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid

actual fun createBackgroundExecutionManager(): BackgroundExecutionManager =
    IosBackgroundExecutionManager()

private class IosBackgroundExecutionManager : BackgroundExecutionManager {
    override fun begin(name: String, onExpired: () -> Unit): BackgroundExecutionLease =
        IosBackgroundExecutionLease(name, onExpired)
}

private class IosBackgroundExecutionLease(
    name: String,
    onExpired: () -> Unit,
) : BackgroundExecutionLease {
    private var identifier: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid

    override val isActive: Boolean
        get() = identifier != UIBackgroundTaskInvalid

    init {
        identifier = UIApplication.sharedApplication.beginBackgroundTaskWithName(name) {
            onExpired()
            end()
        }
    }

    override fun end() {
        val activeIdentifier = identifier
        if (activeIdentifier == UIBackgroundTaskInvalid) return
        identifier = UIBackgroundTaskInvalid
        UIApplication.sharedApplication.endBackgroundTask(activeIdentifier)
    }
}
