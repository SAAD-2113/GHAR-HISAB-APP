package com.sindantechnologies.gharhisab.ui

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.GharHisabApplication
import com.sindantechnologies.gharhisab.MainActivity
import com.sindantechnologies.gharhisab.domain.model.StorageOption
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A full activity smoke flow: actual navigation, preferences, and persisted transaction data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h480dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppNavigationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val container get() = ApplicationProvider.getApplicationContext<GharHisabApplication>().container

    private fun waitForText(text: String) {
        try {
            compose.waitUntil(15_000) {
                shadowOf(Looper.getMainLooper()).idle()
                compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            println("Expected destination: $text\n${compose.onRoot().printToString()}")
            throw failure
        }
        compose.waitForIdle()
    }

    private fun waitForHome() {
        compose.waitUntil(15_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun showQuickActions() {
        // Quick actions live in a horizontal row inside the vertical Home scroller.
        // Bring that outer scroller to the top before scrolling an individual action.
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("View All")))
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -10000f) }
        compose.waitForIdle()
    }

    private fun backHome() {
        compose.onNodeWithContentDescription("Back").performClick()
        waitForHome()
    }

    @Test fun onboardingMainDestinationsAndSettingsSurviveActivityRecreation() {
        waitForText("Choose Your Language")
        compose.onNodeWithText("Continue").performClick()
        waitForText("Choose Theme")
        compose.onNodeWithText("Midnight").performScrollTo().performClick()
        compose.onNodeWithText("Continue").performClick()
        waitForText("Choose Where to Store Your Data")
        compose.onNodeWithText("Google Drive Backup").performScrollTo().performClick()
        compose.onNodeWithText("Continue").performClick()
        waitForText("Connect in Settings")
        compose.onNodeWithText("Continue with Local Storage").performClick()
        waitForHome()
        assertTrue(runBlocking { container.userPreferencesRepository.onboardingCompleteFlow.first() })
        assertEquals(StorageOption.LOCAL, runBlocking { container.userPreferencesRepository.storageOptionFlow.first() })
        assertEquals(ThemeMode.MIDNIGHT, runBlocking { container.userPreferencesRepository.themeModeFlow.first() })

        compose.onNodeWithContentDescription("Add Transaction").performClick()
        waitForText("Save Transaction")
        compose.onNodeWithText("Amount").performTextInput("100.25")
        compose.onNodeWithText("Description (Optional)").performTextInput("Navigation smoke")
        compose.onNodeWithText("Groceries").performScrollTo().performClick()
        compose.onNodeWithText("Save Transaction").performClick()
        waitForHome()
        compose.onNodeWithText("View All").performScrollTo()
        val historyBounds = compose.onNodeWithText("View All").fetchSemanticsNode().boundsInRoot
        val addBounds = compose.onNodeWithContentDescription("Add Transaction").fetchSemanticsNode().boundsInRoot
        assertTrue("Add action must not cover the history link", historyBounds.bottom <= addBounds.top)
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val file = java.io.File("build/reports/navigation/home-small-screen.png")
            requireNotNull(file.parentFile).mkdirs()
            file.outputStream().use { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
        compose.onNodeWithText("View All").performClick()
        waitForText("Transaction History")
        compose.onNodeWithText("Navigation smoke").performScrollTo().performClick()
        waitForText("Edit Transaction")
        waitForText("100.25") // The Room lookup finishes after the destination title is visible.
        compose.onNodeWithText("Amount").assertTextContains("100.25")
        compose.onNodeWithContentDescription("Back").performClick()
        waitForText("Transaction History")
        backHome()

        showQuickActions()
        compose.onNodeWithText("Budgets").performScrollTo().assertIsDisplayed().performClick()
        waitForText("Monthly Overall Budget")
        backHome()
        showQuickActions()
        compose.onNodeWithText("Planned").performScrollTo().assertIsDisplayed().performClick()
        waitForText("Planned expenses")
        compose.onNodeWithContentDescription("Plan an expense").assertIsDisplayed()
        backHome()

        compose.onNodeWithContentDescription("Settings").performClick()
        waitForText("GENERAL")
        compose.onNodeWithText("Starting Available Balance").performScrollTo().performClick()
        compose.onNodeWithText("Available Cash / Balance").performScrollTo().performTextInput("2500.50")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText("Available Cash / Balance").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(250050L, runBlocking { container.userPreferencesRepository.startingBalanceFlow.first() })
        compose.onNodeWithText("Starting Available Balance").performScrollTo().performClick()
        compose.onNodeWithText("Available Cash / Balance").assertTextContains("2,500.50")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Theme").performScrollTo().performClick()
        compose.onNodeWithText("Classic").performScrollTo().performClick()
        compose.onNodeWithText("Apply").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithText("Apply").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(ThemeMode.CLASSIC, runBlocking { container.userPreferencesRepository.themeModeFlow.first() })
        backHome()

        compose.activityRule.scenario.recreate()
        waitForHome()
        compose.onNodeWithText("Choose Your Language").assertDoesNotExist()
        compose.onNodeWithText("Navigation smoke").performScrollTo().assertIsDisplayed()
        val saved = runBlocking { container.transactionRepository.getAllTransactions().first().single() }
        assertEquals(10025L, saved.amount)
        assertEquals(250050L, runBlocking { container.userPreferencesRepository.startingBalanceFlow.first() })
    }
}
