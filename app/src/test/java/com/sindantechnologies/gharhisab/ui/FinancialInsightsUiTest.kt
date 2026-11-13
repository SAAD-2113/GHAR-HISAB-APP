package com.sindantechnologies.gharhisab.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sindantechnologies.gharhisab.domain.model.SpendingForecast
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.ui.screens.home.FinancialInsightsContent
import com.sindantechnologies.gharhisab.ui.theme.GharHisabTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h480dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinancialInsightsUiTest {
    @get:Rule val compose = createComposeRule()
    private val forecast = SpendingForecast(125000, 158000, 1450000, 0,
        LocalDate.of(2026, 11, 25), 12, false, true, true)
    private var permissionRequests = 0
    private lateinit var previewView: View

    private fun launch(dark: Boolean = false, rtl: Boolean = false, fontScale: Float = 1f, permissionGranted: Boolean = true) {
        compose.setContent {
            val view = LocalView.current
            SideEffect { previewView = view.rootView }
            var enabled by remember { mutableStateOf(false) }
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                GharHisabTheme(if (dark) ThemeMode.MIDNIGHT else ThemeMode.CLASSIC) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        FinancialInsightsContent(forecast, enabled, { enabled = it }, permissionGranted, { permissionRequests++ })
                    }
                }
            }
        }
    }

    @Test fun alertsHaveOneAccessibleClickableRowAndToggleBothWays() {
        launch()
        val row = compose.onNodeWithTag("insights_alert_toggle")
        row.performScrollTo().assertIsDisplayed().assertIsOff().performClick().assertIsOn()
        row.performClick().assertIsOff()
        val rowBounds = row.fetchSemanticsNode().boundsInRoot
        assertTrue("Touch target must be at least 48dp", rowBounds.height >= 48f)
    }

    @Test fun largeTextRtlAndMidnightKeepVisualSwitchInsideItsRow() {
        launch(dark = true, rtl = true, fontScale = 1.8f)
        val row = compose.onNodeWithTag("insights_alert_toggle").performScrollTo().assertIsDisplayed()
        val control = compose.onNodeWithTag("insights_alert_control", useUnmergedTree = true).assertIsDisplayed()
        val bounds = row.fetchSemanticsNode().boundsInRoot
        val switch = control.fetchSemanticsNode().boundsInRoot
        assertTrue(switch.left >= bounds.left && switch.right <= bounds.right)
        assertTrue(switch.top >= bounds.top && switch.bottom <= bounds.bottom)
        row.performClick().assertIsOn()
        capturePreview("midnight-rtl-large-text.png")
    }

    @Test fun calculationIsCollapsedUntilRequestedAndCanBeHiddenAgain() {
        launch()
        compose.onNodeWithTag("insights_calculation_details").assertDoesNotExist()
        compose.onNodeWithTag("insights_calculation_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("insights_calculation_details").assertExists()
        compose.onNodeWithTag("insights_calculation_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("insights_calculation_details").assertDoesNotExist()
    }

    @Test fun androidPermissionBlockHasAnExplicitAction() {
        launch(permissionGranted = false)
        compose.onNodeWithTag("insights_alert_toggle").performScrollTo().performClick()
        compose.onNodeWithText("Alerts are selected, but Android notifications are blocked.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Notification settings").performScrollTo().performClick()
        assertEquals(1, permissionRequests)
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp-mdpi")
    fun classicPreviewShowsClearAmountAndExpandedToggleSpace() {
        launch()
        compose.onNodeWithTag("insights_daily_amount").assertIsDisplayed()
        compose.onNodeWithTag("insights_alert_toggle").assertIsDisplayed()
        capturePreview("classic.png")
    }

    private fun capturePreview(name: String) {
        // PixelCopy used by captureToImage waits for a real window frame, which Robolectric
        // does not reliably produce. Drawing the Android root uses its native canvas instead.
        compose.runOnIdle {
            assertTrue(previewView.width > 0 && previewView.height > 0)
            val bitmap = Bitmap.createBitmap(previewView.width, previewView.height, Bitmap.Config.ARGB_8888)
            previewView.draw(Canvas(bitmap))
            val file = File("build/reports/insights", name)
            requireNotNull(file.parentFile).mkdirs()
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
            assertTrue(file.length() > 0)
        }
    }
}
