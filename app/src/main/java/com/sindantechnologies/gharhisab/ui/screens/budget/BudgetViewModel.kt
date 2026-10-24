package com.sindantechnologies.gharhisab.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindantechnologies.gharhisab.data.local.room.entity.BudgetEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.repository.BudgetRepository
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryBudgetProgress(
    val category: CategoryEntity,
    val budget: BudgetEntity,
    val spentPaisa: Long,
    val remainingPaisa: Long,
    val progressPercent: Float,
    val isExceeded: Boolean,
    val isWarning: Boolean
)

data class BudgetUiState(
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val overallBudget: BudgetEntity? = null,
    val overallSpentPaisa: Long = 0L,
    val overallRemainingPaisa: Long = 0L,
    val overallProgressPercent: Float = 0f,
    val isOverallExceeded: Boolean = false,
    val isOverallWarning: Boolean = false,
    val categoryBudgets: List<CategoryBudgetProgress> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true
)

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadBudgetData()
    }

    private fun loadBudgetData() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

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

        viewModelScope.launch {
            combine(
                budgetRepository.getAllBudgetsForMonth(currentMonth, currentYear),
                transactionRepository.getExpenseTotalBetween(startOfMonth, endOfMonth),
                categoryRepository.getActiveCategories(),
                transactionRepository.getTransactionsBetween(startOfMonth, endOfMonth)
            ) { budgets, totalExpense, categories, transactions ->
                val overall = budgets.find { it.categoryId == null }
                val totalSpent = totalExpense ?: 0L

                val overallBudgetAmount = overall?.amount ?: 0L
                val overallRemaining = overallBudgetAmount - totalSpent
                val overallProgress = if (overallBudgetAmount > 0L) {
                    (totalSpent.toFloat() / overallBudgetAmount.toFloat()).coerceIn(0f, 2f)
                } else 0f
                val warningThreshold = overall?.warningThresholdPercent ?: 80

                val categoryProgressList = mutableListOf<CategoryBudgetProgress>()
                val catBudgets = budgets.filter { it.categoryId != null }

                for (b in catBudgets) {
                    val cat = categories.find { it.id == b.categoryId }
                    if (cat != null) {
                        val spentForCat = transactions
                            .filter { it.categoryId == cat.id && it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }
                        val remainingForCat = b.amount - spentForCat
                        val progressForCat = if (b.amount > 0L) {
                            (spentForCat.toFloat() / b.amount.toFloat()).coerceIn(0f, 2f)
                        } else 0f

                        categoryProgressList.add(
                            CategoryBudgetProgress(
                                category = cat,
                                budget = b,
                                spentPaisa = spentForCat,
                                remainingPaisa = remainingForCat,
                                progressPercent = progressForCat,
                                isExceeded = spentForCat > b.amount,
                                isWarning = progressForCat >= (b.warningThresholdPercent / 100f) && spentForCat <= b.amount
                            )
                        )
                    }
                }

                BudgetUiState(
                    month = currentMonth,
                    year = currentYear,
                    overallBudget = overall,
                    overallSpentPaisa = totalSpent,
                    overallRemainingPaisa = overallRemaining,
                    overallProgressPercent = overallProgress,
                    isOverallExceeded = overallBudgetAmount > 0L && totalSpent > overallBudgetAmount,
                    isOverallWarning = overallBudgetAmount > 0L && overallProgress >= (warningThreshold / 100f) && totalSpent <= overallBudgetAmount,
                    categoryBudgets = categoryProgressList,
                    expenseCategories = categories.filter { it.transactionType == TransactionType.EXPENSE },
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setOverallBudget(amountPaisa: Long, warningPercent: Int = 80) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val existing = currentState.overallBudget
            val budget = existing?.copy(
                amount = amountPaisa,
                warningThresholdPercent = warningPercent
            ) ?: BudgetEntity(
                categoryId = null,
                month = currentState.month,
                year = currentState.year,
                amount = amountPaisa,
                warningThresholdPercent = warningPercent
            )
            budgetRepository.insertOrUpdateBudget(budget)
        }
    }

    fun setCategoryBudget(categoryId: Long, amountPaisa: Long, warningPercent: Int = 80) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val existing = currentState.categoryBudgets.find { it.category.id == categoryId }?.budget
            val budget = existing?.copy(
                amount = amountPaisa,
                warningThresholdPercent = warningPercent
            ) ?: BudgetEntity(
                categoryId = categoryId,
                month = currentState.month,
                year = currentState.year,
                amount = amountPaisa,
                warningThresholdPercent = warningPercent
            )
            budgetRepository.insertOrUpdateBudget(budget)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }

    class Factory(
        private val budgetRepository: BudgetRepository,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BudgetViewModel(budgetRepository, transactionRepository, categoryRepository) as T
        }
    }
}
