package com.budgetapp.app.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLockCoordinatorTest {
    @Test
    fun normalBackgroundingStillLocks() {
        val coordinator = AppLockCoordinator()

        assertTrue(coordinator.shouldLockOnStop(isChangingConfigurations = false))
        assertFalse(coordinator.shouldLockOnStop(isChangingConfigurations = true))
    }

    @Test
    fun trustedExternalActivitySuppressesRelockUntilResultReturns() {
        val coordinator = AppLockCoordinator()

        coordinator.begin()
        assertFalse(coordinator.shouldLockOnStop(isChangingConfigurations = false))

        coordinator.end()
        assertTrue(coordinator.shouldLockOnStop(isChangingConfigurations = false))
    }

    @Test
    fun nestedTrustedActivitiesAreBalanced() {
        val coordinator = AppLockCoordinator()

        coordinator.begin()
        coordinator.begin()
        coordinator.end()
        assertTrue(coordinator.isActive)

        coordinator.end()
        coordinator.end()
        assertFalse(coordinator.isActive)
    }
}
