package com.budgetapp.app

import android.os.Bundle
import android.view.WindowManager
import androidx.compose.foundation.background
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
                        LockedScreen(onUnlock = ::unlock)
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
        )
    }

    private companion object {
        const val StateIsUnlocked = "state_is_unlocked"
        const val StateHasRequestedUnlock = "state_has_requested_unlock"
    }
}

@Composable
private fun LockedScreen(
    onUnlock: () -> Unit,
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
            text = "Unlock to view your budget.",
            color = MockTextLightMuted,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
        )
        Button(
            onClick = onUnlock,
            colors = ButtonDefaults.buttonColors(
                containerColor = MockPrimaryAlt,
                contentColor = MockWhite,
            ),
        ) {
            Text("Unlock", fontWeight = FontWeight.Bold)
        }
    }
}
