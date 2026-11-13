package com.sindantechnologies.gharhisab

import android.content.Context
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.ui.screens.transaction.DateRangeFilter
import com.sindantechnologies.gharhisab.ui.screens.transaction.TransactionsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionsFilteringTest {
    private lateinit var db: GharHisabDatabase
    private lateinit var viewModel: TransactionsViewModel
    private val store = ViewModelStore()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), GharHisabDatabase::class.java)
            .allowMainThreadQueries().build()
        runBlocking { db.categoryDao().insertCategories(listOf(
            CategoryEntity(1, "Groceries", TransactionType.EXPENSE, "shopping_cart"),
            CategoryEntity(2, "Salary", TransactionType.INCOME, "payments"),
        )) }
        viewModel = ViewModelProvider(store, TransactionsViewModel.Factory(
            TransactionRepository(db.transactionDao()), CategoryRepository(db.categoryDao()),
        ))[TransactionsViewModel::class.java]
    }

    @After fun teardown() {
        val job = viewModel.viewModelScope.coroutineContext[Job]!!
        store.clear()
        awaitState { job.isCompleted }
        db.close()
    }

    private fun awaitState(condition: () -> Boolean) = runBlocking {
        withTimeout(10_000) {
            while (!condition()) { shadowOf(Looper.getMainLooper()).idle(); delay(10) }
        }
    }

    private fun insert(timestamp: Long, description: String, amount: Long = 100L, type: TransactionType = TransactionType.EXPENSE) = runBlocking {
        db.transactionDao().insertTransaction(TransactionEntity(
            type = type, amount = amount, categoryId = if (type == TransactionType.INCOME) 2 else 1,
            description = description, timestamp = timestamp, paymentMethod = PaymentMethod.CASH,
        ))
    }

    @Test fun thisMonthIncludesBothBoundariesAndExcludesAdjacentMonths() {
        val first = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val next = LocalDate.now().withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        insert(first - 1, "Previous month", 50000)
        val firstId = insert(first, "First instant", 10025)
        val lastId = insert(next - 1, "Last instant", 20075)
        insert(next, "Next month", 90000)
        awaitState { viewModel.uiState.value.transactions.size == 4 }
        viewModel.onDateRangeFilterSelected(DateRangeFilter.THIS_MONTH)
        awaitState { viewModel.uiState.value.selectedDateRange == DateRangeFilter.THIS_MONTH }
        assertEquals(setOf(firstId, lastId), viewModel.uiState.value.transactions.map { it.id }.toSet())
        assertEquals(30100L, viewModel.uiState.value.filteredExpenseTotalPaisa)
        viewModel.onDateRangeFilterSelected(DateRangeFilter.ALL)
        awaitState { viewModel.uiState.value.transactions.size == 4 }
        assertEquals("All time must still expose deliberately future-dated records", 170100L, viewModel.uiState.value.filteredExpenseTotalPaisa)
    }

    @Test fun lastThirtyDaysExcludesFutureRecordsAndCombinesWithSearchAndType() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        insert(now - 31 * day, "Old groceries", 90000)
        val recentId = insert(now - 29 * day, "Weekly food", 25050)
        insert(now - day, "Monthly pay", 500000, TransactionType.INCOME)
        insert(now + day, "Future groceries", 70000)
        awaitState { viewModel.uiState.value.transactions.size == 4 }
        viewModel.onDateRangeFilterSelected(DateRangeFilter.LAST_30_DAYS)
        awaitState { viewModel.uiState.value.selectedDateRange == DateRangeFilter.LAST_30_DAYS }
        assertEquals(2, viewModel.uiState.value.transactions.size)
        assertEquals(25050L, viewModel.uiState.value.filteredExpenseTotalPaisa)
        assertEquals(500000L, viewModel.uiState.value.filteredIncomeTotalPaisa)
        viewModel.onTypeFilterSelected(TransactionType.EXPENSE)
        viewModel.onSearchQueryChanged("GROCERIES")
        awaitState { viewModel.uiState.value.searchQuery == "GROCERIES" && viewModel.uiState.value.selectedType == TransactionType.EXPENSE }
        assertEquals(recentId, viewModel.uiState.value.transactions.single().id)
        assertEquals(0L, viewModel.uiState.value.filteredIncomeTotalPaisa)
        viewModel.onSearchQueryChanged("no match")
        awaitState { viewModel.uiState.value.searchQuery == "no match" }
        assertTrue(viewModel.uiState.value.transactions.isEmpty())
        assertEquals(0L, viewModel.uiState.value.filteredExpenseTotalPaisa)
    }
}
