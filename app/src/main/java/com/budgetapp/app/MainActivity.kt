package com.budgetapp.app

import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.budgetapp.app.navigation.BudgetNavHost
import com.budgetapp.app.security.BiometricGate
import com.budgetapp.app.security.AppLockCoordinator
import com.budgetapp.app.security.LocalTrustedExternalActivityCoordinator
import com.budgetapp.app.ui.theme.BudgetTheme
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockWhite

class MainActivity : FragmentActivity() {
    private lateinit var biometricGate: BiometricGate
    private var isUnlocked by mutableStateOf(false)
    private var isAuthenticating = false
    private var hasRequestedInitialUnlock = false
    private var lockNotice by mutableStateOf<String?>(null)
    private val appLockCoordinator = AppLockCoordinator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restored so a rotation (or process death) does not drop the user back to the lock screen
        // mid-task.
        isUnlocked = savedInstanceState?.getBoolean(StateIsUnlocked) ?: false
        hasRequestedInitialUnlock = savedInstanceState?.getBoolean(StateHasRequestedUnlock) ?: false
        if (!BuildConfig.ALLOW_SCREEN_CAPTURE) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }

        biometricGate = BiometricGate(this)

        setContent {
            BudgetTheme {
                CompositionLocalProvider(
                    LocalTrustedExternalActivityCoordinator provides appLockCoordinator,
                ) {
                    if (isUnlocked) {
                        BudgetNavHost()
                    } else {
                        LockedScreen(
                            onUnlock = ::unlock,
                            notice = lockNotice,
                            onOpenSecuritySettings = ::openSecuritySettings,
                            onContinueWithoutLock = {
                                lockNotice = null
                                isUnlocked = true
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isUnlocked && !hasRequestedInitialUnlock) {
            hasRequestedInitialUnlock = true
            unlock()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(StateIsUnlocked, isUnlocked)
        outState.putBoolean(StateHasRequestedUnlock, hasRequestedInitialUnlock)
    }

    override fun onStop() {
        super.onStop()
        if (appLockCoordinator.shouldLockOnStop(isChangingConfigurations)) {
            isUnlocked = false
            hasRequestedInitialUnlock = false
        }
    }

    private fun unlock() {
        if (isAuthenticating) return
        isAuthenticating = true
        biometricGate.authenticate(
            onUnlocked = {
                isAuthenticating = false
                isUnlocked = true
            },
            onLocked = {
                isAuthenticating = false
                isUnlocked = false
            },
            onUnavailable = { message ->
                isAuthenticating = false
                isUnlocked = false
                lockNotice = message
            },
        )
    }

    private fun openSecuritySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            .onFailure { lockNotice = "Could not open security settings." }
    }

    private companion object {
        const val StateIsUnlocked = "state_is_unlocked"
        const val StateHasRequestedUnlock = "state_has_requested_unlock"
    }
}

@Composable
private fun LockedScreen(
    onUnlock: () -> Unit,
    notice: String?,
    onOpenSecuritySettings: () -> Unit,
    onContinueWithoutLock: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MockSurface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Chap",
            color = MockWhite,
            fontSize = 38.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = notice ?: "Unlock to view your budget.",
            color = MockTextLightMuted,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
        )
        Button(
            onClick = if (notice == null) onUnlock else onOpenSecuritySettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = MockPrimaryAlt,
                contentColor = MockWhite,
            ),
        ) {
            Text(if (notice == null) "Unlock" else "Open security settings", fontWeight = FontWeight.Bold)
        }
        if (notice != null) {
            Text(
                text = "Continue without the lock",
                color = MockPrimaryAlt,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clickable(onClick = onContinueWithoutLock)
                    .padding(12.dp),
            )
            Text(
                text = "Your budget stays encrypted on this device either way.",
                color = MockTextLightMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
