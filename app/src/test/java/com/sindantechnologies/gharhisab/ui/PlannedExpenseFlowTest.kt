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
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.PlannedExpenseRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.ui.screens.planned.PlannedExpensesScreen
import com.sindantechnologies.gharhisab.ui.screens.planned.PlannedExpensesViewModel
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

/** User-visible plan editing and payment flows backed by the real Room repositories. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h480dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlannedExpenseFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: GharHisabDatabase
    private lateinit var viewModel: PlannedExpensesViewModel
    private lateinit var repository: PlannedExpenseRepository
    private val store = ViewModelStore()

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GharHisabDatabase::class.java).allowMainThreadQueries().build()
        runBlocking { db.categoryDao().insertCategories(listOf(CategoryEntity(1, "Rent", TransactionType.EXPENSE, "home"))) }
        repository = PlannedExpenseRepository(db)
        viewModel = ViewModelProvider(store, PlannedExpensesViewModel.Factory(
            repository, TransactionRepository(db.transactionDao()), CategoryRepository(db.categoryDao()),
            UserPreferencesRepository(context), context,
        ))[PlannedExpensesViewModel::class.java]
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
                GharHisabTheme(ThemeMode.MIDNIGHT) { PlannedExpensesScreen(viewModel, {}) }
            }
        }
        awaitState { !viewModel.uiState.value.isLoading }
    }

    private fun seedPlan() = runBlocking {
        repository.insertPlannedExpense(PlannedExpenseEntity(
            description = "Apartment rent", expectedAmount = 250050, categoryId = 1,
            plannedDate = System.currentTimeMillis(), recurrence = "NONE",
        ))
    }

    @Test fun creatingRecurringPlanAtLargeTextDoesNotRecordAnActualExpense() {
        launch(fontScale = 1.3f)
        compose.onNodeWithContentDescription("Plan an expense").performClick()
        compose.onNodeWithText("Title").performScrollTo().performTextInput("Apartment rent")
        compose.onNodeWithText("Amount").performScrollTo().performTextInput("2500.50")
        compose.onNodeWithText("Monthly").performScrollTo().performClick()
        compose.onNodeWithText("Repeat every (interval)").performScrollTo().performTextReplacement("2")
        compose.onNodeWithText("Save Transaction").assertIsDisplayed().assertIsEnabled().performClick()
        awaitState { viewModel.uiState.value.operationSerial == 1 && viewModel.uiState.value.plannedExpenses.size == 1 }
        val plan = runBlocking { db.plannedExpenseDao().getAllPlannedExpensesList().single() }
        assertEquals("Apartment rent", plan.description)
        assertEquals(250050L, plan.expectedAmount)
        assertEquals("MONTHLY", plan.recurrence)
        assertEquals(2, plan.recurrenceInterval)
        assertEquals(PlannedExpenseStatus.PLANNED, plan.status)
        assertTrue(runBlocking { db.transactionDao().getAllTransactionsList().isEmpty() })
    }

    @Test fun markingPlanPaidUsesTheActualAmountAndPaymentMethod() {
        val id = seedPlan()
        launch()
        compose.onNodeWithText("Mark as paid").performScrollTo().performClick()
        compose.onNodeWithText("Amount").performScrollTo().performTextReplacement("2499.75")
        compose.onNodeWithText("Bank").performScrollTo().performClick()
        compose.onNodeWithText("OK").assertIsEnabled().performClick()
        awaitState { viewModel.uiState.value.plannedExpenses.singleOrNull()?.status == PlannedExpenseStatus.PAID && !viewModel.uiState.value.isSaving }
        val transaction = runBlocking { db.transactionDao().getAllTransactionsList().single() }
        val plan = runBlocking { db.plannedExpenseDao().getPlannedExpenseById(id)!! }
        assertEquals(249975L, transaction.amount)
        assertEquals(PaymentMethod.BANK, transaction.paymentMethod)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals(id, transaction.plannedExpenseId)
        assertEquals(transaction.id, plan.actualTransactionId)
        assertEquals("The quoted amount remains available after recording an actual payment", 250050L, plan.expectedAmount)
        compose.onNodeWithText("Mark as paid").assertDoesNotExist()
    }

    @Test fun cancellingAndDeletingUnpaidPlanDoesNotCreateAnExpense() {
        seedPlan()
        launch()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        awaitState { viewModel.uiState.value.plannedExpenses.singleOrNull()?.status == PlannedExpenseStatus.CANCELLED }
        compose.onNodeWithText("Mark as paid").assertDoesNotExist()
        compose.onNodeWithText("Delete").performScrollTo().performClick()
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Apartment rent").assertExists()
        compose.onNodeWithText("Delete").performScrollTo().performClick()
        compose.onNode(hasText("Delete") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        awaitState { viewModel.uiState.value.plannedExpenses.isEmpty() }
        assertTrue(runBlocking { db.transactionDao().getAllTransactionsList().isEmpty() })
    }

    @Test fun deletingPaidPlanPreservesItsRecordedExpense() {
        val id = seedPlan()
        val transactionId = runBlocking { repository.markAsPaid(id) }
        launch()
        compose.onNodeWithText("Delete").performScrollTo().performClick()
        compose.onNode(hasText("Delete") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        awaitState { viewModel.uiState.value.plannedExpenses.isEmpty() }
        val transaction = runBlocking { db.transactionDao().getAllTransactionsList().single() }
        assertEquals(transactionId, transaction.id)
        assertEquals(250050L, transaction.amount)
    }
}
