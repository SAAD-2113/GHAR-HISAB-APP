package com.sindantechnologies.gharhisab

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.sindantechnologies.gharhisab.domain.model.AppLockMode
import com.sindantechnologies.gharhisab.domain.model.AppLockTimeout
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.ui.navigation.GharHisabNavHost
import com.sindantechnologies.gharhisab.ui.screens.security.LockScreen
import com.sindantechnologies.gharhisab.ui.theme.GharHisabTheme
import com.sindantechnologies.gharhisab.util.reminder.NotificationHelper

/**
 * Single-activity host for the GharHisab application.
 *
 * Observes the current [ThemeMode] from DataStore and wraps
 * the entire navigation graph in [GharHisabTheme].
 */
class MainActivity : FragmentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    private var isAppLocked by mutableStateOf(true)
    private var lastBackgroundTimeMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as GharHisabApplication

        setContent {
            val themeMode by app.container.userPreferencesRepository.themeModeFlow
                .collectAsState(initial = ThemeMode.FOLLOW_SYSTEM)

            val lockMode by app.container.userPreferencesRepository.appLockModeFlow
                .collectAsState(initial = AppLockMode.OFF)

            val pinSalt by app.container.userPreferencesRepository.appLockPinSaltFlow
                .collectAsState(initial = "")

            val pinHash by app.container.userPreferencesRepository.appLockPinHashFlow
                .collectAsState(initial = "")

            GharHisabTheme(themeMode = themeMode) {
                if (isAppLocked && lockMode != AppLockMode.OFF) {
                    LockScreen(
                        lockMode = lockMode,
                        pinSalt = pinSalt,
                        pinHash = pinHash,
                        onUnlock = { isAppLocked = false }
                    )
                } else {
                    GharHisabNavHost(
                        appContainer = app.container
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundTimeMs = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (lastBackgroundTimeMs > 0L) {
            isAppLocked = true
        }
    }
}
