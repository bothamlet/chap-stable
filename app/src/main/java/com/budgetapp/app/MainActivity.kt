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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
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
    private val unlockState: AppUnlockState by viewModels()
    private var isUnlocked: Boolean
        get() = unlockState.isUnlocked
        set(value) { unlockState.isUnlocked = value }
    private var isAuthenticating = false
    private var hasRequestedInitialUnlock = false
    private var lockNotice by mutableStateOf<String?>(null)
    private var canContinueWithoutLock by mutableStateOf(false)
    private val appLockCoordinator = AppLockCoordinator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The ViewModel survives rotation; a new process must authenticate again.
        if (!BuildConfig.ALLOW_SCREEN_CAPTURE) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }

        biometricGate = BiometricGate(this, appLockCoordinator)

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
                            canContinueWithoutLock = canContinueWithoutLock,
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

    override fun onStop() {
        super.onStop()
        if (appLockCoordinator.shouldLockOnStop(isChangingConfigurations)) {
            isUnlocked = false
            hasRequestedInitialUnlock = false
        }
    }

    private fun unlock() {
        if (isAuthenticating) return
        lockNotice = null
        canContinueWithoutLock = false
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
            onUnavailable = { message, mayContinue ->
                isAuthenticating = false
                isUnlocked = false
                lockNotice = message
                canContinueWithoutLock = mayContinue
            },
        )
    }

    private fun openSecuritySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            .onFailure { lockNotice = "Could not open security settings." }
    }

}

class AppUnlockState : ViewModel() {
    var isUnlocked by mutableStateOf(false)
}

@Composable
private fun LockedScreen(
    onUnlock: () -> Unit,
    notice: String?,
    canContinueWithoutLock: Boolean,
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
                text = "Try unlocking again",
                color = MockPrimaryAlt,
                modifier = Modifier.clickable(onClick = onUnlock).padding(12.dp),
            )
        }
        if (canContinueWithoutLock) {
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
