package com.sindantechnologies.gharhisab

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.lifecycleScope
import com.sindantechnologies.gharhisab.domain.model.*
import com.sindantechnologies.gharhisab.ui.navigation.GharHisabNavHost
import com.sindantechnologies.gharhisab.ui.screens.security.LockScreen
import com.sindantechnologies.gharhisab.ui.theme.GharHisabTheme
import com.sindantechnologies.gharhisab.util.reminder.ReminderScheduler
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    val backups by lazy {
        val container = (application as GharHisabApplication).container
        val authorization = com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(applicationContext)
        androidx.lifecycle.ViewModelProvider(this, object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                com.sindantechnologies.gharhisab.ui.screens.backup.BackupViewModel(container.backupManager,
                    invalidateToken = { token ->
                        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                            authorization.clearToken(com.google.android.gms.auth.api.identity.ClearTokenRequest.builder().setToken(token).build())
                                .addOnSuccessListener { if (continuation.isActive) continuation.resumeWith(Result.success(Unit)) }
                                .addOnFailureListener { if (continuation.isActive) continuation.resumeWith(Result.failure(it)) }
                                .addOnCanceledListener { if (continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException("Token invalidation cancelled"))) }
                        }
                    }) as T
        })[com.sindantechnologies.gharhisab.ui.screens.backup.BackupViewModel::class.java]
    }
    val external by lazy { androidx.lifecycle.ViewModelProvider(this)[ExternalActions::class.java] }
    val googleAuthorization = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()) {
        // Even RESULT_CANCELED can carry a useful Google API error. Preserve it for parsing.
        external.recordGoogleResult(it.resultCode, it.data)
    }
    val importDocument = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { external.importUri = it }
    val exportDocument = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")) {
        external.exportUri = it
        if (it == null) external.exportPassword = ""
    }
    private var recovered by mutableStateOf(false)
    private var recoveryFailed by mutableStateOf(false)
    private val session by lazy { androidx.lifecycle.ViewModelProvider(this)[com.sindantechnologies.gharhisab.util.security.LockSession::class.java] }
    private var covered by mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.sindantechnologies.gharhisab.util.security.BiometricAuthManager.reattach(this)
        enableEdgeToEdge()
        // Protect first frame, before asynchronous security preferences have loaded.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val container = (application as GharHisabApplication).container
        lifecycleScope.launch {
            try { container.backupManager.recoverInterruptedRestore(); recovered = true }
            catch (_: Exception) { recoveryFailed = true }
        }
        setContent {
            val recoveryBlocked by container.backupManager.recoveryBlocked.collectAsState()
            val navigation = androidx.navigation.compose.rememberNavController()
            val security by container.userPreferencesRepository.securityState.collectAsState(initial = null)
            val language by container.userPreferencesRepository.languageFlow.collectAsState(initial = Language.ENGLISH)
            val theme by container.userPreferencesRepository.themeModeFlow.collectAsState(initial = ThemeMode.FOLLOW_SYSTEM)
            SideEffect {
                session.timeout = security?.timeout ?: 0
                if (security?.mode == AppLockMode.OFF) { session.locked = false; window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
                else window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            val localizedContext = remember(language) {
                android.view.ContextThemeWrapper(this@MainActivity, R.style.Theme_GharHisab).apply {
                    applyOverrideConfiguration(android.content.res.Configuration(this@MainActivity.resources.configuration).apply { setLocale(java.util.Locale(language.code)) })
                }
            }
            SideEffect { java.util.Locale.setDefault(java.util.Locale(language.code)) }
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration,
                androidx.compose.ui.platform.LocalLayoutDirection provides if (language == Language.URDU) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr,
            ) {
            GharHisabTheme(theme) {
                val state = security
                val blocked = recoveryBlocked || !recovered || state == null || covered || (state.mode != AppLockMode.OFF && session.locked)
                androidx.activity.compose.BackHandler(enabled = blocked) { moveTaskToBack(true) }
                when {
                    recoveryFailed || recoveryBlocked -> Surface(Modifier.fillMaxSize()) { androidx.compose.material3.Text(getString(R.string.restore_startup_failed)) }
                    !recovered || state == null || covered -> Surface(Modifier.fillMaxSize()) {}
                    state.mode != AppLockMode.OFF && session.locked -> LockScreen(container.userPreferencesRepository, state.mode) { deviceAuthenticationSucceeded() }
                    else -> GharHisabNavHost(container, navigation)
                }
            }
            }
        }
    }
    fun deviceAuthenticationSucceeded() {
        session.backgroundAt = null
        session.locked = false
    }
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) session.leave(SystemClock.elapsedRealtime())
        covered = true
    }
    override fun onStart() {
        super.onStart()
        session.enter(SystemClock.elapsedRealtime())
        covered = false
        val container = (application as GharHisabApplication).container
        lifecycleScope.launch {
            runCatching { ReminderScheduler.reconcile(this@MainActivity, container.plannedExpenseRepository.getActivePlannedExpensesList()) }
        }
    }
}
