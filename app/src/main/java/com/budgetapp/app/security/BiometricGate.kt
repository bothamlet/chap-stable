package com.budgetapp.app.security

import android.os.Build
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.budgetapp.app.BuildConfig

class BiometricGate(
    private val activity: FragmentActivity,
    private val externalActivities: TrustedExternalActivityCoordinator,
) {
    private var legacyOnUnlocked: (() -> Unit)? = null
    private var legacyOnLocked: (() -> Unit)? = null
    private val credentialLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        externalActivities.end()
        if (result.resultCode == Activity.RESULT_OK) legacyOnUnlocked?.invoke() else legacyOnLocked?.invoke()
        legacyOnUnlocked = null
        legacyOnLocked = null
    }

    fun authenticate(
        onUnlocked: () -> Unit,
        onLocked: () -> Unit,
        onUnavailable: (String, Boolean) -> Unit,
    ) {
        if (canBypassForEmulator()) {
            onUnlocked()
            return
        }

        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isDeviceSecure) {
            onUnavailable("No screen lock is set up on this device.", true)
            return
        }
        // Strong biometrics combined with device credentials are unsupported on API 28/29.
        // The system credential dialog works across every legacy version we support.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val intent = keyguard.createConfirmDeviceCredentialIntent("Unlock Chap", "Use your device PIN or password")
            if (intent == null) {
                onUnavailable("Could not open the device unlock screen.", false)
                return
            }
            legacyOnUnlocked = onUnlocked
            legacyOnLocked = onLocked
            externalActivities.launchTrustedExternalActivity { credentialLauncher.launch(intent) }
            return
        }

        val allowedAuthenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val biometricManager = BiometricManager.from(activity)

        when (val status = biometricManager.canAuthenticate(allowedAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> showPrompt(
                allowedAuthenticators = allowedAuthenticators,
                onUnlocked = onUnlocked,
                onLocked = onLocked,
                onUnavailable = onUnavailable,
            )
            // Nothing to authenticate against: say so instead of leaving the user on a lock
            // screen whose only button re-runs the same failing check.
            else -> onUnavailable(managerUnavailableMessage(status), false)
        }
    }

    private fun managerUnavailableMessage(status: Int): String =
        when (status) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "No fingerprint, face or screen lock is set up on this device."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "This device has no biometric sensor."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "The biometric sensor is unavailable right now."
            else ->
                "Unlocking is unavailable on this device."
        }

    private fun showPrompt(
        allowedAuthenticators: Int,
        onUnlocked: () -> Unit,
        onLocked: () -> Unit,
        onUnavailable: (String, Boolean) -> Unit,
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Chap")
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
                    when (errorCode) {
                        BiometricPrompt.ERROR_NO_BIOMETRICS ->
                            onUnavailable("No fingerprint or face is enrolled on this device.", false)
                        BiometricPrompt.ERROR_HW_NOT_PRESENT ->
                            onUnavailable("This device has no biometric sensor.", false)
                        else -> onLocked()
                    }
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
