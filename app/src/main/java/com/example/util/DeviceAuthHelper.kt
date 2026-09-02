package com.example.util

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object DeviceAuthHelper {

    /**
     * Checks if the device has either Biometrics (Fingerprint/Face) or Device Lock (PIN/Pattern/Password) set up.
     */
    fun canAuthenticateWithDevice(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        val canAuth = biometricManager.canAuthenticate(authenticators)
        
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            return true
        }

        // Fallback check on KeyguardManager
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    /**
     * Returns true if biometric hardware is available and enrolled
     */
    fun hasBiometricHardware(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Prompt device authentication using native Biometric + Device PIN/Pattern fallback.
     */
    fun promptDeviceAuthentication(
        activity: FragmentActivity,
        title: String = "Unlock DropQR",
        subtitle: String = "Use your device fingerprint, face, or device PIN/Pattern",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // User canceled or other non-fatal dismissals
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError("Authentication canceled")
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Kept inside the system dialog, but can log if needed
                }
            }
        )

        try {
            val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(authenticators)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            // If device credential + biometric combined is not supported on older API levels, fallback
            fallbackKeyguardAuth(activity, title, onSuccess, onError)
        }
    }

    private fun fallbackKeyguardAuth(
        activity: FragmentActivity,
        title: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
            // No lock configured on device, allow access
            onSuccess()
            return
        }

        try {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                title,
                "Confirm your device lock (PIN, Pattern, or Password)"
            )
            if (intent != null) {
                activity.startActivity(intent)
            } else {
                onSuccess()
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to open device lock screen")
        }
    }

    fun openDeviceSecuritySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open security settings", Toast.LENGTH_SHORT).show()
        }
    }
}
