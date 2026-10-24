package com.sindantechnologies.gharhisab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.ui.navigation.GharHisabNavHost
import com.sindantechnologies.gharhisab.ui.theme.GharHisabTheme

/**
 * Single-activity host for the GharHisab application.
 *
 * Observes the current [ThemeMode] from DataStore and wraps
 * the entire navigation graph in [GharHisabTheme].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
