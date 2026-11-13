package com.sindantechnologies.gharhisab.util.security

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Android owns the device credentials; this app never reads or stores them. */
object BiometricAuthManager {
    // This is an application access gate, not a CryptoObject operation. Permit enrolled
    // device biometrics (including face) and the OS PIN/pattern/password on API 29+.
    internal const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    class AuthenticationState : ViewModel() {
        var inProgress by mutableStateOf(false)
            internal set
        var failed by mutableStateOf(false)
            internal set
        internal var success: (() -> Unit)? = null
        internal var error: ((String) -> Unit)? = null
        internal var nonMatch: (() -> Unit)? = null
        internal fun clear() { inProgress = false; success = null; error = null; nonMatch = null }
        override fun onCleared() { clear() }
    }

    fun state(activity: FragmentActivity): AuthenticationState = ViewModelProvider(activity)[AuthenticationState::class.java]

    fun canAuthenticate(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true ||
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    /** Call from Activity.onCreate to reattach to an OS prompt surviving rotation. */
    fun reattach(activity: FragmentActivity) { prompt(activity, state(activity)) }

    private fun prompt(activity: FragmentActivity, session: AuthenticationState): BiometricPrompt = BiometricPrompt(
        activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (!session.inProgress) return
                val callback = session.success
                session.clear()
                session.failed = false
                callback?.invoke()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!session.inProgress) return
                val callback = session.error
                session.clear()
                session.failed = true
                callback?.invoke(errString.toString())
            }
            override fun onAuthenticationFailed() {
                // A non-matching finger is not a terminal error: Android keeps the prompt open.
                if (session.inProgress) { session.failed = true; session.nonMatch?.invoke() }
            }
        },
    )

    fun promptAuthentication(
        activity: FragmentActivity,
        title: String = "Unlock GharHisab",
        subtitle: String = "Use your fingerprint, face or device screen lock",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onNonMatch: () -> Unit = {},
    ) {
        val session = state(activity)
        if (session.inProgress) return
        session.success = onSuccess; session.error = onError; session.nonMatch = onNonMatch
        session.failed = false
        session.inProgress = true
        try {
            prompt(activity, session).authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle(title).setSubtitle(subtitle).setAllowedAuthenticators(AUTHENTICATORS).build())
        } catch (_: Exception) {
            session.clear()
            session.failed = true
            onError("Device authentication is unavailable. Check your Android screen-lock settings.")
        }
    }
}

fun findActivity(context: Context): FragmentActivity? = when (context) {
    is FragmentActivity -> context
    is android.content.ContextWrapper -> findActivity(context.baseContext)
    else -> null
}
