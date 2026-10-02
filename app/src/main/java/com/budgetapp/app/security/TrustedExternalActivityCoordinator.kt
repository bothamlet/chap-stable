package com.budgetapp.app.security

import androidx.compose.runtime.staticCompositionLocalOf

interface TrustedExternalActivityCoordinator {
    val isActive: Boolean

    fun begin()

    fun end()
}

class AppLockCoordinator : TrustedExternalActivityCoordinator {
    private var activeExternalActivities = 0

    override val isActive: Boolean
        get() = activeExternalActivities > 0

    override fun begin() {
        activeExternalActivities += 1
    }

    override fun end() {
        activeExternalActivities = (activeExternalActivities - 1).coerceAtLeast(0)
    }

    fun shouldLockOnStop(isChangingConfigurations: Boolean): Boolean =
        !isChangingConfigurations && !isActive
}

val LocalTrustedExternalActivityCoordinator =
    staticCompositionLocalOf<TrustedExternalActivityCoordinator> {
        object : TrustedExternalActivityCoordinator {
            override val isActive = false

            override fun begin() = Unit

            override fun end() = Unit
        }
    }

inline fun TrustedExternalActivityCoordinator.launchTrustedExternalActivity(
    launch: () -> Unit,
) {
    begin()
    try {
        launch()
    } catch (error: Throwable) {
        end()
        throw error
    }
}
