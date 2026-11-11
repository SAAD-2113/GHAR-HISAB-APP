package com.sindantechnologies.gharhisab.ui

import android.content.Context
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.ui.screens.transaction.AddTransactionScreen
import com.sindantechnologies.gharhisab.ui.screens.transaction.AddTransactionViewModel
import com.sindantechnologies.gharhisab.ui.screens.transaction.EditTransactionScreen
import com.sindantechnologies.gharhisab.ui.screens.transaction.EditTransactionViewModel
import com.sindantechnologies.gharhisab.ui.theme.GharHisabTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h480dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TransactionFormTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: GharHisabDatabase
    private lateinit var viewModel: AddTransactionViewModel
    private val store = ViewModelStore()
    private lateinit var view: View

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), GharHisabDatabase::class.java)
            .allowMainThreadQueries().build()
        runBlocking {
            db.categoryDao().insertCategories((1L..12L).map {
                CategoryEntity(it, if (it == 12L) "Other Expense" else "Category $it", TransactionType.EXPENSE, "home")
            } + CategoryEntity(13L, "Salary", TransactionType.INCOME, "payments"))
        }
        viewModel = ViewModelProvider(store, AddTransactionViewModel.Factory(
            TransactionRepository(db.transactionDao()), CategoryRepository(db.categoryDao())
        ))[AddTransactionViewModel::class.java]
    }

    @After fun teardown() {
        compose.runOnIdle { store.clear() }
        db.close()
    }

    private fun awaitState(condition: () -> Boolean) {
        compose.waitUntil(10_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun launch(fontScale: Float = 1f, rtl: Boolean = false, dark: Boolean = false) {
        compose.setContent {
            view = LocalView.current
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                GharHisabTheme(if (dark) ThemeMode.MIDNIGHT else ThemeMode.CLASSIC) {
                    AddTransactionScreen(viewModel, {})
                }
            }
        }
        awaitState { viewModel.uiState.value.categories.size == 12 }
    }

    private fun insets(ime: Int = 0) {
        compose.runOnIdle {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 48))
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, ime))
                .setVisible(WindowInsetsCompat.Type.ime(), ime > 0)
                .build()
            ViewCompat.dispatchApplyWindowInsets(view, insets)
        }
        compose.waitForIdle()
    }

    private fun assertActionInsideScreen(bottomInset: Int, label: String = "Save Transaction") {
        val action = compose.onNodeWithText(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Entire action must fit above system UI: $action / $screen", action.bottom <= screen.bottom - bottomInset + 1)
        assertTrue(action.top >= 0 && action.left >= 0 && action.right <= screen.right)
    }

    @Test fun expenseSaveIsVisibleAndPersistsExactAmount() {
        launch()
        insets()
        assertActionInsideScreen(48)
        compose.onNodeWithText("Amount").performTextInput("1250.50")
        compose.onNodeWithText("Other Expense").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Save Transaction").performClick()
        awaitState { viewModel.uiState.value.isSaved }
        val saved = runBlocking { db.transactionDao().getAllTransactionsList().single() }
        assertEquals(125050L, saved.amount)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals(12L, saved.categoryId)
    }

    @Test fun incomeSaveIsVisibleAndPersists() {
        launch()
        compose.onNodeWithText("Income").performClick()
        compose.onNodeWithText("Amount").performTextInput("25000")
        compose.onNodeWithText("Salary").performScrollTo().performClick()
        insets()
        assertActionInsideScreen(48)
        compose.onNodeWithText("Save Transaction").performClick()
        awaitState { viewModel.uiState.value.isSaved }
        val saved = runBlocking { db.transactionDao().getAllTransactionsList().single() }
        assertEquals(TransactionType.INCOME, saved.type)
        assertEquals(2500000L, saved.amount)
    }

    @Test fun keyboardAndLargeTextLeaveLastCategoryReachableInRtlDarkTheme() {
        launch(fontScale = 1.6f, rtl = true, dark = true)
        compose.onNodeWithText("Description (Optional)").performScrollTo().performTextInput("Test")
        insets(ime = 180)
        assertActionInsideScreen(180)
        compose.onNodeWithText("Other Expense").performScrollTo().assertIsDisplayed().performClick()
        insets()
        assertActionInsideScreen(48)
    }

    @Test fun repeatedSaveRequestsCreateOnlyOneTransaction() {
        launch()
        compose.runOnIdle {
            viewModel.onAmountChanged("75.25")
            viewModel.onCategorySelected(viewModel.uiState.value.categories.first())
            viewModel.saveTransaction()
            viewModel.saveTransaction()
        }
        awaitState { viewModel.uiState.value.isSaved }
        compose.runOnIdle { viewModel.saveTransaction() }
        assertEquals(1, runBlocking { db.transactionDao().getAllTransactionsList().size })
    }

    @Test fun failedDatabaseWritePreservesFormAndCanBeRetried() {
        launch()
        val category = viewModel.uiState.value.categories.first()
        compose.runOnIdle {
            viewModel.onAmountChanged("75.25")
            viewModel.onDescriptionChanged("Keep this text")
            viewModel.onCategorySelected(category)
        }
        // Remove the referenced category to cause a real SQLite foreign-key error.
        db.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE id = ${category.id}")
        compose.runOnIdle { viewModel.saveTransaction() }
        awaitState { viewModel.uiState.value.saveFailed }
        assertFalse(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Keep this text", viewModel.uiState.value.description)
        assertEquals("75.25", viewModel.uiState.value.amountText)
        assertTrue(runBlocking { db.transactionDao().getAllTransactionsList().isEmpty() })
        runBlocking { db.categoryDao().insertCategories(listOf(category)) }
        compose.onNodeWithText("Save Transaction").performClick()
        awaitState { viewModel.uiState.value.isSaved }
        assertEquals(7525L, runBlocking { db.transactionDao().getAllTransactionsList().single().amount })
    }

    private fun launchEdit(): EditTransactionViewModel {
        val id = runBlocking {
            db.transactionDao().insertTransaction(TransactionEntity(
                type = TransactionType.EXPENSE, amount = 10000L, categoryId = 1L,
                description = "Existing", timestamp = System.currentTimeMillis(), paymentMethod = null,
            ))
        }
        val edit = ViewModelProvider(store, EditTransactionViewModel.Factory(
            id, TransactionRepository(db.transactionDao()), CategoryRepository(db.categoryDao())
        ))[EditTransactionViewModel::class.java]
        compose.setContent {
            view = LocalView.current
            GharHisabTheme { EditTransactionScreen(edit, {}) }
        }
        awaitState { !edit.uiState.value.isLoading }
        return edit
    }

    @Test fun editUpdatesStoredAmountWithoutCreatingAnotherTransaction() {
        val edit = launchEdit()
        insets()
        compose.onNodeWithText("Amount").performTextReplacement("500.25")
        compose.onNodeWithText("Update Transaction").assertIsDisplayed().performClick()
        awaitState { edit.uiState.value.isSaved }
        assertEquals(50025L, runBlocking { db.transactionDao().getAllTransactionsList().single().amount })
    }

    @Test fun deleteRequiresConfirmationAndRemovesTransaction() {
        val edit = launchEdit()
        compose.onNodeWithContentDescription("Delete").performClick()
        assertEquals(1, runBlocking { db.transactionDao().getAllTransactionsList().size })
        compose.onNodeWithText("Delete", substring = false).performClick()
        awaitState { edit.uiState.value.isDeleted }
        assertTrue(runBlocking { db.transactionDao().getAllTransactionsList().isEmpty() })
    }

    @Test @Config(qualifiers = "ur-rPK-w320dp-h480dp-mdpi")
    fun urduFormKeepsTranslatedActionVisible() {
        launch(fontScale = 1.3f, rtl = true)
        insets()
        assertActionInsideScreen(48, "لین دین محفوظ کریں")
        compose.onNodeWithText("Other Expense").performScrollTo().assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w640dp-h360dp-land-mdpi")
    fun landscapeKeepsActionAndLastFieldAccessible() {
        launch()
        insets()
        assertActionInsideScreen(48)
        compose.onNodeWithText("Other Expense").performScrollTo().assertIsDisplayed()
    }
}
