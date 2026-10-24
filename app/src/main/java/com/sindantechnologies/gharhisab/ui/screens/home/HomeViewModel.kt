package com.sindantechnologies.gharhisab.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.data.local.room.entity.BudgetEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.data.repository.BudgetRepository
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.PlannedExpenseRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository
import com.sindantechnologies.gharhisab.domain.model.DailySpendingRecommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

/**
 * UI state for the Home Dashboard screen.
 */
data class HomeUiState(
    val monthName: String = "",
    val totalIncome: Long = 0L,              // Current month income in paisa
    val totalExpense: Long = 0L,             // Current month expense in paisa
    val netMonthlyCashFlow: Long = 0L,       // Net cash flow this month
    val availableFunds: Long = 0L,           // Current available funds (starting balance + all-time net)
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val allCategoriesList: List<CategoryEntity> = emptyList(),
    val overallBudget: BudgetEntity? = null,
    val remainingBudget: Long = 0L,
    val budgetProgressPercent: Float = 0f,
    val isBudgetExceeded: Boolean = false,
    val isBudgetWarning: Boolean = false,
    val upcomingPlannedExpenses: List<PlannedExpenseEntity> = emptyList(),
    val dailyRecommendation: DailySpendingRecommendation = DailySpendingRecommendation.calculate(0L, 0L, 0L),
    val isLoading: Boolean = true,
)

private data class MonthFlowData(
    val incomePaisa: Long,
    val expensePaisa: Long,
    val transactions: List<TransactionEntity>,
    val overallBudget: BudgetEntity?
)

private data class AllTimeFlowData(
    val allTimeIncome: Long,
    val allTimeExpense: Long,
    val startingBalance: Long,
    val protectedReserve: Long,
    val upcomingPlans: List<PlannedExpenseEntity>
)

/**
 * ViewModel for the Home Dashboard screen.
 */
class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val plannedExpenseRepository: PlannedExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        val monthStr = "${DateFormatSymbols(Locale.getDefault()).months[currentMonth - 1]} $currentYear"

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        val now = System.currentTimeMillis()

        val monthDataFlow = combine(
            transactionRepository.getIncomeTotalBetween(startOfMonth, endOfMonth),
            transactionRepository.getExpenseTotalBetween(startOfMonth, endOfMonth),
            transactionRepository.getTransactionsBetween(startOfMonth, endOfMonth),
            budgetRepository.getOverallBudget(currentMonth, currentYear)
        ) { income, expense, transactions, budget ->
            MonthFlowData(
                incomePaisa = income ?: 0L,
                expensePaisa = expense ?: 0L,
                transactions = transactions,
                overallBudget = budget
            )
        }

        val allTimeDataFlow = combine(
            transactionRepository.getAllTimeIncomeTotal(),
            transactionRepository.getAllTimeExpenseTotal(),
            userPreferencesRepository.startingBalanceFlow,
            userPreferencesRepository.protectedReserveFlow,
            plannedExpenseRepository.getUpcomingPlannedExpenses()
        ) { allIncome, allExpense, startingBalance, protectedReserve, upcomingPlans ->
            AllTimeFlowData(
                allTimeIncome = allIncome ?: 0L,
                allTimeExpense = allExpense ?: 0L,
                startingBalance = startingBalance,
                protectedReserve = protectedReserve,
                upcomingPlans = upcomingPlans
            )
        }

        viewModelScope.launch {
            combine(
                monthDataFlow,
                allTimeDataFlow,
                categoryRepository.getActiveCategories()
            ) { monthData, allTimeData, categories ->
                val incomePaisa = monthData.incomePaisa
                val expensePaisa = monthData.expensePaisa
                val netCashFlow = incomePaisa - expensePaisa

                val allTimeNet = allTimeData.allTimeIncome - allTimeData.allTimeExpense
                val currentAvailableFunds = allTimeData.startingBalance + allTimeNet

                val categoryMap = categories.associateBy { it.id }

                val upcomingInMonth = allTimeData.upcomingPlans.filter { it.plannedDate in now..endOfMonth }
                val plannedCommitmentsPaisa = upcomingInMonth.sumOf { it.expectedAmount }

                val recommendation = DailySpendingRecommendation.calculate(
                    currentAvailableFundsPaisa = currentAvailableFunds,
                    upcomingPlannedCommitmentsPaisa = plannedCommitmentsPaisa,
                    protectedReservePaisa = allTimeData.protectedReserve
                )

                val budgetAmount = monthData.overallBudget?.amount ?: 0L
                val remainingBudget = budgetAmount - expensePaisa
                val budgetProgress = if (budgetAmount > 0L) {
                    (expensePaisa.toFloat() / budgetAmount.toFloat()).coerceIn(0f, 2f)
                } else 0f
                val warningThreshold = (monthData.overallBudget?.warningThresholdPercent ?: 80) / 100f

                HomeUiState(
                    monthName = monthStr,
                    totalIncome = incomePaisa,
                    totalExpense = expensePaisa,
                    netMonthlyCashFlow = netCashFlow,
                    availableFunds = currentAvailableFunds,
                    recentTransactions = monthData.transactions.take(5),
                    categories = categoryMap,
                    allCategoriesList = categories,
                    overallBudget = monthData.overallBudget,
                    remainingBudget = remainingBudget,
                    budgetProgressPercent = budgetProgress,
                    isBudgetExceeded = budgetAmount > 0L && expensePaisa > budgetAmount,
                    isBudgetWarning = budgetAmount > 0L && budgetProgress >= warningThreshold && expensePaisa <= budgetAmount,
                    upcomingPlannedExpenses = allTimeData.upcomingPlans.take(3),
                    dailyRecommendation = recommendation,
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    suspend fun saveVoiceTransaction(transaction: TransactionEntity) {
        transactionRepository.insertTransaction(transaction)
    }

    /** Factory for manual dependency injection. */
    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val budgetRepository: BudgetRepository,
        private val plannedExpenseRepository: PlannedExpenseRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                transactionRepository,
                categoryRepository,
                budgetRepository,
                plannedExpenseRepository,
                userPreferencesRepository
            ) as T
        }
    }
}
