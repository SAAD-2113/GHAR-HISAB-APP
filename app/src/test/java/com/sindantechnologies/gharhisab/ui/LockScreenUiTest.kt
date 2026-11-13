package com.sindantechnologies.gharhisab.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.AppLockMode
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.ui.screens.security.LockScreen
import com.sindantechnologies.gharhisab.ui.theme.GharHisabTheme
import com.sindantechnologies.gharhisab.util.security.PinVerifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Host UI verification only; biometric hardware and AndroidKeyStore need a real phone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LockScreenUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pinValidationProgressAndRecoveryAreClearAndUsable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val release = CountDownLatch(1)
        val checks = AtomicInteger()
        val repository = UserPreferencesRepository(context, object : PinVerifier {
            override fun create(pin: String, salt: ByteArray) = "v3k:test-only"
            override fun matches(pin: String, salt: ByteArray, expected: String): Boolean {
                checks.incrementAndGet()
                check(release.await(20, TimeUnit.SECONDS))
                return pin == "1234"
            }
        })
        val recovery = runBlocking { repository.clearAppLock(); repository.setAppLockPin("1234") }
        var unlocks = 0
        lateinit var preview: View
        compose.setContent {
            val view = LocalView.current
            SideEffect { preview = view.rootView }
            GharHisabTheme(ThemeMode.CLASSIC) { LockScreen(repository, AppLockMode.PIN) { unlocks++ } }
        }
        try {
            val unlockLabel = context.getString(R.string.lock_unlock)
            compose.onNodeWithText(unlockLabel).assertIsDisplayed().assertIsNotEnabled()
            compose.runOnIdle {
                val bitmap = Bitmap.createBitmap(preview.width, preview.height, Bitmap.Config.ARGB_8888)
                preview.draw(Canvas(bitmap))
                val file = File("build/reports/security/lock-classic.png")
                requireNotNull(file.parentFile).mkdirs()
                file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                bitmap.recycle()
            }
            compose.onNode(hasSetTextAction()).performTextInput("000")
            compose.onNodeWithText(unlockLabel).assertIsNotEnabled()
            compose.onNode(hasSetTextAction()).performTextInput("0")
            compose.onNodeWithText(unlockLabel).assertIsEnabled().performClick()
            compose.waitUntil(10_000) { idle(); checks.get() == 1 }
            compose.onNodeWithText(context.getString(R.string.security_verifying)).assertIsDisplayed().assertIsNotEnabled()
            compose.onNodeWithText(context.getString(R.string.lock_pin)).assertIsNotEnabled()
            release.countDown()
            compose.waitUntil(10_000) {
                idle()
                compose.onAllNodesWithText(context.getString(R.string.lock_failed)).fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(0, unlocks)
            compose.onNodeWithText(context.getString(R.string.security_recovery_action)).performClick()
            compose.onNodeWithText(context.getString(R.string.security_recovery_help)).performScrollTo().assertIsDisplayed()
            compose.onNode(hasSetTextAction()).performScrollTo().performTextInput(recovery)
            compose.onNodeWithText(unlockLabel).performScrollTo().performClick()
            compose.waitUntil(10_000) { idle(); unlocks == 1 }
            assertEquals(AppLockMode.OFF, runBlocking { repository.securityState.first().mode })
        } finally { release.countDown() }
    }

    private fun idle() { org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle() }
}
