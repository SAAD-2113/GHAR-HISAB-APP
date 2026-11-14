package com.sindantechnologies.gharhisab.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sindantechnologies.gharhisab.domain.model.ThemeMode

/**
 * Classic (Light) color scheme.
 */
private val ClassicColorScheme = lightColorScheme(
    primary = DeepRed,
    onPrimary = OnPrimary,
    primaryContainer = ClassicPrimaryContainer,
    onPrimaryContainer = ClassicOnPrimaryContainer,
    secondary = RoyalGold,
    onSecondary = OnSecondary,
    secondaryContainer = ClassicSecondaryContainer,
    onSecondaryContainer = ClassicOnSecondaryContainer,
    tertiary = RoyalGoldLight,
    tertiaryContainer = ClassicTertiaryContainer,
    onTertiaryContainer = ClassicOnTertiaryContainer,
    background = ClassicBackground,
    onBackground = ClassicOnBackground,
    surface = ClassicSurface,
    onSurface = ClassicOnSurface,
    surfaceVariant = ClassicSurfaceVariant,
    onSurfaceVariant = ClassicOnSurfaceVariant,
    outline = ClassicOutline,
    error = ClassicError,
    onError = ClassicOnError,
    errorContainer = ClassicErrorContainer,
    onErrorContainer = ClassicOnErrorContainer,
)

/**
 * Midnight (Dark) color scheme.
 */
private val MidnightColorScheme = darkColorScheme(
    primary = DeepRed,
    onPrimary = OnPrimary,
    primaryContainer = MidnightPrimaryContainer,
    onPrimaryContainer = MidnightOnPrimaryContainer,
    secondary = RoyalGold,
    onSecondary = OnSecondary,
    secondaryContainer = MidnightSecondaryContainer,
    onSecondaryContainer = MidnightOnSecondaryContainer,
    tertiary = RoyalGoldLight,
    tertiaryContainer = MidnightTertiaryContainer,
    onTertiaryContainer = MidnightOnTertiaryContainer,
    background = MidnightBackground,
    onBackground = MidnightOnBackground,
    surface = MidnightSurface,
    onSurface = MidnightOnSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightOnSurfaceVariant,
    outline = MidnightOutline,
    error = MidnightError,
    onError = MidnightOnError,
    errorContainer = MidnightErrorContainer,
    onErrorContainer = MidnightOnErrorContainer,
)

/**
 * GharHisab application theme.
 *
 * @param themeMode The user-selected theme mode (Classic, Midnight, or FollowSystem).
 */
@Composable
fun GharHisabTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.CLASSIC -> false
        ThemeMode.MIDNIGHT -> true
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) MidnightColorScheme else ClassicColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GharHisabTypography,
        shapes = GharHisabShapes,
        content = content,
    )
}
