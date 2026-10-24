package com.sindantechnologies.gharhisab

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.ui.navigation.GharHisabNavHost
import com.sindantechnologies.gharhisab.ui.theme.GharHisabTheme
import com.sindantechnologies.gharhisab.util.reminder.NotificationHelper

/**
 * Single-activity host for the GharHisab application.
 *
 * Observes the current [ThemeMode] from DataStore and wraps
 * the entire navigation graph in [GharHisabTheme].
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

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

            GharHisabTheme(themeMode = themeMode) {
                GharHisabNavHost(
                    appContainer = app.container
                )
            }
        }
    }
}
