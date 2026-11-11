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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
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
    val forecast: com.sindantechnologies.gharhisab.domain.model.SpendingForecast? = null,
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun loadDashboardData() {
        val ticker = kotlinx.coroutines.flow.flow {
            while (true) { emit(java.time.LocalDate.now()); kotlinx.coroutines.delay(60_000) }
        }
        val preferences = combine(userPreferencesRepository.startingBalanceFlow, userPreferencesRepository.protectedReserveFlow) { balance, reserve -> balance to reserve }
        val records = combine(transactionRepository.getAllTransactions(), plannedExpenseRepository.getAllPlannedExpenses(), categoryRepository.getActiveCategories()) { tx, plans, cats -> Triple(tx, plans, cats) }
        val datedBudget = ticker.flatMapLatest { date -> budgetRepository.getOverallBudget(date.monthValue, date.year).map { date to it } }
        viewModelScope.launch {
            combine(records, preferences, datedBudget) { records, preferences, today -> Triple(records, preferences, today) }
                .collect { (records, preferences, datedBudget) ->
                    val (today, budget) = datedBudget
                    val (transactions, plans, categories) = records
                    val (starting, reserve) = preferences
                    val zone = java.time.ZoneId.systemDefault()
                    val current = transactions.filter { java.time.Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() <= today }
                    val monthly = current.filter { java.time.YearMonth.from(java.time.Instant.ofEpochMilli(it.timestamp).atZone(zone)) == java.time.YearMonth.from(today) }
                    fun total(rows: List<TransactionEntity>, type: com.sindantechnologies.gharhisab.domain.model.TransactionType) = rows.filter { it.type == type }.fold(0L) { acc, tx -> Math.addExact(acc, tx.amount) }
                    val income = total(monthly, com.sindantechnologies.gharhisab.domain.model.TransactionType.INCOME)
                    val expense = total(monthly, com.sindantechnologies.gharhisab.domain.model.TransactionType.EXPENSE)
                    val allIncome = total(current, com.sindantechnologies.gharhisab.domain.model.TransactionType.INCOME)
                    val allExpense = total(current, com.sindantechnologies.gharhisab.domain.model.TransactionType.EXPENSE)
                    val balance = Math.subtractExact(Math.addExact(starting, allIncome), allExpense)
                    val forecast = com.sindantechnologies.gharhisab.domain.model.SpendingForecaster.calculate(current, plans, balance, reserve,
                        starting > 0 || current.any { it.type == com.sindantechnologies.gharhisab.domain.model.TransactionType.INCOME }, today, zone, budget?.let { it.amount - expense })
                    val recommendation = DailySpendingRecommendation.calculate(balance, forecast.commitments, reserve).copy(dailyLimitPaisa = forecast.dailyLimit)
                    val progress = if (budget != null && budget.amount > 0) expense.toFloat() / budget.amount else 0f
                    _uiState.value = HomeUiState(
                        monthName = today.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()) + " " + today.year,
                        totalIncome = income, totalExpense = expense, netMonthlyCashFlow = income - expense,
                        availableFunds = balance, recentTransactions = monthly.take(5), categories = categories.associateBy { it.id }, allCategoriesList = categories,
                        overallBudget = budget, remainingBudget = (budget?.amount ?: 0) - expense, budgetProgressPercent = progress,
                        isBudgetExceeded = budget != null && expense > budget.amount,
                        isBudgetWarning = budget != null && progress >= budget.warningThresholdPercent / 100f,
                        upcomingPlannedExpenses = plans.filter { it.status == com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus.PLANNED }.take(3),
                        dailyRecommendation = recommendation, forecast = forecast, isLoading = false)
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
