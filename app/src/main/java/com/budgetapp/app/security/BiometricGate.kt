package com.budgetapp.app.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.budgetapp.app.BuildConfig

class BiometricGate(
    private val activity: FragmentActivity,
) {
    fun authenticate(
        onUnlocked: () -> Unit,
        onLocked: () -> Unit,
    ) {
        if (canBypassForEmulator()) {
            onUnlocked()
            return
        }

        val allowedAuthenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val biometricManager = BiometricManager.from(activity)

        when (biometricManager.canAuthenticate(allowedAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> showPrompt(
                allowedAuthenticators = allowedAuthenticators,
                onUnlocked = onUnlocked,
                onLocked = onLocked,
            )
            else -> if (canBypassForEmulator()) {
                onUnlocked()
            } else {
                onLocked()
            }
        }
    }

    private fun showPrompt(
        allowedAuthenticators: Int,
        onUnlocked: () -> Unit,
        onLocked: () -> Unit,
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Budget")
            .setSubtitle("Use biometrics or your device PIN")
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    onLocked()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    onUnlocked()
                }
            },
        )

        prompt.authenticate(promptInfo)
    }

    private fun canBypassForEmulator(): Boolean =
        BuildConfig.ALLOW_EMULATOR_BIOMETRIC_BYPASS && isRunningOnEmulator()

    private fun isRunningOnEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk", ignoreCase = true) ||
            Build.DEVICE.contains("emulator", ignoreCase = true) ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.PRODUCT == "google_sdk"
}
