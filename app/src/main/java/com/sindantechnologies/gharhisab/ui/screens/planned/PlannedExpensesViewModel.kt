package com.sindantechnologies.gharhisab.ui.screens.planned

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.PlannedExpenseRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.util.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

data class PlannedExpensesUiState(
    val plannedExpenses: List<PlannedExpenseEntity> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val expectedExpensesThisWeekPaisa: Long = 0L,
    val expectedExpensesThisMonthPaisa: Long = 0L,
    val totalPlannedSpendingPaisa: Long = 0L,
    val currentAvailableFundsPaisa: Long = 0L,
    val expectedRemainingFundsPaisa: Long = 0L,
    val isLoading: Boolean = true
)

class PlannedExpensesViewModel(
    private val plannedExpenseRepository: PlannedExpenseRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannedExpensesUiState())
    val uiState: StateFlow<PlannedExpensesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val now = System.currentTimeMillis()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val endOfWeek = cal.timeInMillis

        val calMonth = Calendar.getInstance()
        calMonth.set(Calendar.DAY_OF_MONTH, calMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
        calMonth.set(Calendar.HOUR_OF_DAY, 23)
        calMonth.set(Calendar.MINUTE, 59)
        calMonth.set(Calendar.SECOND, 59)
        val endOfMonth = calMonth.timeInMillis

        viewModelScope.launch {
            combine(
                plannedExpenseRepository.getAllPlannedExpenses(),
                categoryRepository.getActiveCategories(),
                transactionRepository.getAllTimeIncomeTotal(),
                transactionRepository.getAllTimeExpenseTotal(),
                userPreferencesRepository.startingBalanceFlow
            ) { plannedList, catList, totalIncome, totalExpense, startingBalance ->
                val catMap = catList.associateBy { it.id }

                val unpaidPlans = plannedList.filter { it.status == PlannedExpenseStatus.PLANNED }

                val thisWeekPaisa = unpaidPlans
                    .filter { it.plannedDate in now..endOfWeek }
                    .sumOf { it.expectedAmount }

                val thisMonthPaisa = unpaidPlans
                    .filter { it.plannedDate in now..endOfMonth }
                    .sumOf { it.expectedAmount }

                val totalPlannedPaisa = unpaidPlans.sumOf { it.expectedAmount }

                val netIncome = (totalIncome ?: 0L) - (totalExpense ?: 0L)
                val availableFunds = startingBalance + netIncome
                val expectedRemaining = availableFunds - thisMonthPaisa

                PlannedExpensesUiState(
                    plannedExpenses = plannedList,
                    categories = catMap,
                    expenseCategories = catList.filter { it.transactionType == TransactionType.EXPENSE },
                    expectedExpensesThisWeekPaisa = thisWeekPaisa,
                    expectedExpensesThisMonthPaisa = thisMonthPaisa,
                    totalPlannedSpendingPaisa = totalPlannedPaisa,
                    currentAvailableFundsPaisa = availableFunds,
                    expectedRemainingFundsPaisa = expectedRemaining,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun addPlannedExpense(
        description: String,
        amountPaisa: Long,
        categoryId: Long,
        plannedDate: Long,
        notes: String?,
        recurrence: String? = "NONE"
    ) {
        viewModelScope.launch {
            val item = PlannedExpenseEntity(
                description = description,
                expectedAmount = amountPaisa,
                categoryId = categoryId,
                plannedDate = plannedDate,
                notes = notes,
                recurrence = recurrence,
                status = PlannedExpenseStatus.PLANNED
            )
            val newId = plannedExpenseRepository.insertPlannedExpense(item)
            ReminderScheduler.scheduleReminders(context, item.copy(id = newId))
        }
    }

    fun markAsPaid(plannedExpenseId: Long, paymentMethod: PaymentMethod = PaymentMethod.CASH) {
        viewModelScope.launch {
            plannedExpenseRepository.markAsPaid(
                plannedExpenseId = plannedExpenseId,
                paymentMethod = paymentMethod
            )
            ReminderScheduler.cancelReminders(context, plannedExpenseId)
        }
    }

    fun cancelPlannedExpense(id: Long) {
        viewModelScope.launch {
            plannedExpenseRepository.cancelPlannedExpense(id)
            ReminderScheduler.cancelReminders(context, id)
        }
    }

    fun reschedulePlannedExpense(id: Long, newDate: Long) {
        viewModelScope.launch {
            val existing = plannedExpenseRepository.getPlannedExpenseById(id) ?: return@launch
            val updated = existing.copy(
                plannedDate = newDate,
                updatedAt = System.currentTimeMillis()
            )
            plannedExpenseRepository.updatePlannedExpense(updated)
            ReminderScheduler.scheduleReminders(context, updated)
        }
    }

    fun deletePlannedExpense(item: PlannedExpenseEntity) {
        viewModelScope.launch {
            plannedExpenseRepository.deletePlannedExpense(item)
            ReminderScheduler.cancelReminders(context, item.id)
        }
    }

    class Factory(
        private val plannedExpenseRepository: PlannedExpenseRepository,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val context: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlannedExpensesViewModel(
                plannedExpenseRepository,
                transactionRepository,
                categoryRepository,
                userPreferencesRepository,
                context
            ) as T
        }
    }
}
