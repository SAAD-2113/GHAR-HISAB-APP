package com.sindantechnologies.gharhisab.ui.screens.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateRangeFilter(val displayName: String) {
    ALL("All Time"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days")
}

private data class FilterState(
    val query: String,
    val type: TransactionType?,
    val categoryId: Long?,
    val dateRange: DateRangeFilter
)

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val allCategoriesList: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedCategoryId: Long? = null,
    val selectedDateRange: DateRangeFilter = DateRangeFilter.ALL,
    val filteredIncomeTotalPaisa: Long = 0L,
    val filteredExpenseTotalPaisa: Long = 0L,
    val isLoading: Boolean = true,
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedDateRange = MutableStateFlow(DateRangeFilter.ALL)

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val filterFlow = combine(
            _searchQuery,
            _selectedType,
            _selectedCategoryId,
            _selectedDateRange
        ) { query, type, catId, dateRange ->
            FilterState(query, type, catId, dateRange)
        }

        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getActiveCategories(),
                filterFlow
            ) { allTransactions, categories, filters ->
                val categoryMap = categories.associateBy { it.id }

                val now = System.currentTimeMillis()
                val minTimestamp = when (filters.dateRange) {
                    DateRangeFilter.ALL -> 0L
                    DateRangeFilter.THIS_MONTH -> {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                    DateRangeFilter.LAST_30_DAYS -> now - (30L * 24 * 60 * 60 * 1000)
                }

                val filtered = allTransactions.filter { item ->
                    val matchesDate = item.timestamp >= minTimestamp
                    val matchesType = filters.type == null || item.type == filters.type
                    val matchesCategory = filters.categoryId == null || item.categoryId == filters.categoryId
                    val matchesQuery = filters.query.isBlank() ||
                            (item.description?.contains(filters.query, ignoreCase = true) == true) ||
                            (categoryMap[item.categoryId]?.name?.contains(filters.query, ignoreCase = true) == true)

                    matchesDate && matchesType && matchesCategory && matchesQuery
                }

                val incomeTotal = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val expenseTotal = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                TransactionsUiState(
                    transactions = filtered,
                    categories = categoryMap,
                    allCategoriesList = categories,
                    searchQuery = filters.query,
                    selectedType = filters.type,
                    selectedCategoryId = filters.categoryId,
                    selectedDateRange = filters.dateRange,
                    filteredIncomeTotalPaisa = incomeTotal,
                    filteredExpenseTotalPaisa = expenseTotal,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onTypeFilterSelected(type: TransactionType?) {
        _selectedType.value = type
    }

    fun onCategoryFilterSelected(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun onDateRangeFilterSelected(range: DateRangeFilter) {
        _selectedDateRange.value = range
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(transactionRepository, categoryRepository) as T
        }
    }
}
