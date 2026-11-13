package com.sindantechnologies.gharhisab.ui

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.data.local.room.entity.BudgetEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.data.repository.BudgetRepository
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.ui.screens.budget.BudgetScreen
import com.sindantechnologies.gharhisab.ui.screens.budget.BudgetViewModel
import com.sindantechnologies.gharhisab.ui.theme.GharHisabTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

/** Exercises the real budget editor, Room writes, and monthly/category totals together. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h480dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BudgetFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: GharHisabDatabase
    private lateinit var viewModel: BudgetViewModel
    private val store = ViewModelStore()
    private val today = LocalDate.now()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), GharHisabDatabase::class.java)
            .allowMainThreadQueries().build()
        runBlocking {
            db.categoryDao().insertCategories(listOf(
                CategoryEntity(1, "Groceries", TransactionType.EXPENSE, "shopping_cart"),
                CategoryEntity(2, "Transport", TransactionType.EXPENSE, "directions_car"),
                CategoryEntity(3, "Salary", TransactionType.INCOME, "payments"),
            ))
        }
        viewModel = ViewModelProvider(store, BudgetViewModel.Factory(
            BudgetRepository(db.budgetDao()), TransactionRepository(db.transactionDao()), CategoryRepository(db.categoryDao()),
        ))[BudgetViewModel::class.java]
    }

    @After fun teardown() {
        val job = viewModel.viewModelScope.coroutineContext[Job]!!
        compose.runOnIdle { store.clear() }
        awaitState { job.isCompleted }
        db.close()
    }

    private fun awaitState(condition: () -> Boolean) {
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); condition() }
    }

    private fun launch(fontScale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                GharHisabTheme(ThemeMode.CLASSIC) { BudgetScreen(viewModel, {}) }
            }
        }
        awaitState { !viewModel.uiState.value.isLoading }
    }

    @Test fun editingWarningThresholdPreservesAnExistingBudgetsPaisa() {
        runBlocking { db.budgetDao().insertBudget(BudgetEntity(
            month = today.monthValue, year = today.year, amount = 125050, warningThresholdPercent = 67,
        )) }
        launch()
        awaitState { viewModel.uiState.value.overallBudget != null }
        compose.onNodeWithContentDescription("Edit Overall Budget").performScrollTo().performClick()
        compose.onNodeWithText("Warning Threshold (%)").performScrollTo().performTextReplacement("68")
        compose.onNodeWithText("Save").performClick()
        awaitState { viewModel.uiState.value.overallBudget?.warningThresholdPercent == 68 }
        val saved = runBlocking { db.budgetDao().getAllBudgetsList().single() }
        assertEquals("Saving an unchanged budget must not lose fractional money", 125050L, saved.amount)
        assertEquals(68, saved.warningThresholdPercent)
    }

    @Test fun categoryBudgetTracksOnlyThisMonthAndItsExpenseCategory() {
        val current = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val previous = today.withDayOfMonth(1).minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runBlocking {
            listOf(
                TransactionEntity(type = TransactionType.EXPENSE, amount = 50025, categoryId = 1, description = "Food", timestamp = current, paymentMethod = PaymentMethod.CASH),
                TransactionEntity(type = TransactionType.EXPENSE, amount = 20000, categoryId = 2, description = "Bus", timestamp = current, paymentMethod = PaymentMethod.CASH),
                TransactionEntity(type = TransactionType.INCOME, amount = 900000, categoryId = 3, description = "Income", timestamp = current, paymentMethod = PaymentMethod.BANK),
                TransactionEntity(type = TransactionType.EXPENSE, amount = 99900, categoryId = 1, description = "Last month", timestamp = previous, paymentMethod = PaymentMethod.CASH),
            ).forEach { db.transactionDao().insertTransaction(it) }
        }
        launch()
        compose.onNodeWithText("Add Category").performScrollTo().performClick()
        compose.onNodeWithText("Category Budget Amount").performScrollTo().performTextInput("1000.50")
        compose.onNodeWithText("Save").performClick()
        awaitState { viewModel.uiState.value.categoryBudgets.size == 1 }
        val state = viewModel.uiState.value
        val category = state.categoryBudgets.single()
        assertEquals(70025L, state.overallSpentPaisa)
        assertEquals(1L, category.category.id)
        assertEquals(100050L, category.budget.amount)
        assertEquals(50025L, category.spentPaisa)
        assertEquals(50025L, category.remainingPaisa)
        assertEquals(0.5f, category.progressPercent, 0.0001f)
        compose.onNodeWithContentDescription("Delete").performScrollTo().performClick()
        awaitState { viewModel.uiState.value.categoryBudgets.isEmpty() }
        assertTrue(runBlocking { db.budgetDao().getAllBudgetsList().isEmpty() })
        assertEquals("Deleting a budget must not delete the underlying transactions", 4,
            runBlocking { db.transactionDao().getAllTransactionsList().size })
    }

    @Test fun largeTextEditorKeepsFieldsReachableAndRejectsInvalidWarningThresholds() {
        launch(fontScale = 1.4f)
        compose.onNodeWithContentDescription("Edit Overall Budget").performScrollTo().performClick()
        compose.onNodeWithText("Monthly Budget Amount").performScrollTo().performTextInput("5000.75")
        val warning = compose.onNodeWithText("Warning Threshold (%)")
        warning.performScrollTo().assertIsDisplayed().performTextReplacement("101")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        warning.performTextReplacement("0")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        warning.performTextReplacement("85")
        compose.onNodeWithText("Save").assertIsDisplayed().assertIsEnabled().performClick()
        awaitState { viewModel.uiState.value.overallBudget?.amount == 500075L }
        assertEquals(85, runBlocking { db.budgetDao().getAllBudgetsList().single().warningThresholdPercent })
    }
}
