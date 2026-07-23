package com.zhousl.aether.platform

actual fun createBackgroundExecutionManager(): BackgroundExecutionManager =
    object : BackgroundExecutionManager {
        override fun begin(name: String, onExpired: () -> Unit): BackgroundExecutionLease =
            object : BackgroundExecutionLease {
                override var isActive: Boolean = true
                    private set

                override fun end() {
                    isActive = false
                }
            }
    }
