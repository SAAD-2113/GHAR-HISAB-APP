package com.sindantechnologies.gharhisab

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h480dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Version3StartupTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun freshInstallationLoadsOnboardingWithoutRequiringLock() {
        compose.waitUntil(15_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            compose.onAllNodesWithText("Choose Your Language").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Choose Your Language").assertIsDisplayed()
    }
}
