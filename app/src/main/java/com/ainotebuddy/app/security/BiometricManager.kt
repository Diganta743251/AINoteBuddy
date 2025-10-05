package com.ainotebuddy.app.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager as XBiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ainotebuddy.app.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Handles biometric authentication for securing notes
 */
@Singleton
class BiometricManager @Inject constructor(
    private val context: Context
) {
    private val biometricManager = XBiometricManager.from(context)

    /**
     * Check if biometric authentication is available on the device
     * @return Pair of (isAvailable, errorMessage)
     */
    fun isBiometricAvailable(): Pair<Boolean, Int> {
        return when (biometricManager.canAuthenticate(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)) {
            XBiometricManager.BIOMETRIC_SUCCESS -> 
                true to R.string.biometric_available
            XBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> 
                false to R.string.biometric_error_no_hardware
            XBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> 
                false to R.string.biometric_error_hw_unavailable
            XBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> 
                false to R.string.biometric_error_none_enrolled
            XBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                false to R.string.biometric_error_security_update_required
            XBiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                false to R.string.biometric_status_unknown
            XBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                false to R.string.biometric_error_unsupported
            else -> 
                false to R.string.biometric_status_unknown
        }
    }

    /**
     * Show biometric authentication prompt
     * @param activity The activity to show the prompt from
     * @param title Title for the authentication dialog
     * @param subtitle Subtitle for the authentication dialog
     * @param onSuccess Callback when authentication is successful
     * @param onError Callback when authentication fails or is cancelled
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = context.getString(R.string.biometric_auth_required_title),
        subtitle: String = context.getString(R.string.biometric_auth_required_subtitle),
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
            .setConfirmationRequired(true)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError(-1, context.getString(R.string.biometric_authentication_failed))
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Suspend function to authenticate with biometrics
     * @return true if authentication was successful, false otherwise
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = context.getString(R.string.biometric_auth_required_title),
        subtitle: String = context.getString(R.string.biometric_auth_required_subtitle)
    ): Boolean = suspendCoroutine { continuation ->
        showBiometricPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = { continuation.resume(true) },
            onError = { _, _ -> continuation.resume(false) }
        )
    }

    companion object {
        /**
         * Check if the device supports biometric authentication
         */
        fun isBiometricSupported(context: Context): Boolean {
            val biometricManager = XBiometricManager.from(context)
            return biometricManager.canAuthenticate(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL) == 
                   XBiometricManager.BIOMETRIC_SUCCESS
        }
    }
}
