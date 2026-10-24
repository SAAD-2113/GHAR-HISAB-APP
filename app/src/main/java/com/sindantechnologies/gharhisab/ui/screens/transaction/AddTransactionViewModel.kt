package com.sindantechnologies.gharhisab.ui.screens.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddTransactionUiState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val description: String = "",
    val selectedCategory: CategoryEntity? = null,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val timestamp: Long = System.currentTimeMillis(),
    val categories: List<CategoryEntity> = emptyList(),
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getActiveCategories().collect { allCategories ->
                val filteredCategories = allCategories.filter {
                    it.transactionType == _uiState.value.transactionType
                }
                _uiState.update { it.copy(categories = filteredCategories) }
            }
        }
    }

    fun onTransactionTypeChanged(type: TransactionType) {
        _uiState.update {
            it.copy(
                transactionType = type,
                selectedCategory = null,
                categoryError = null,
            )
        }
        loadCategories()
    }

    fun onAmountChanged(amount: String) {
        // Allow only digits and one decimal point
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val dotCount = filtered.count { it == '.' }
        if (dotCount > 1) return
        // Limit decimal places to 2
        val parts = filtered.split(".")
        if (parts.size == 2 && parts[1].length > 2) return

        _uiState.update { it.copy(amountText = filtered, amountError = null) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onCategorySelected(category: CategoryEntity) {
        _uiState.update { it.copy(selectedCategory = category, categoryError = null) }
    }

    fun onPaymentMethodSelected(method: PaymentMethod) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun onDateChanged(timestamp: Long) {
        _uiState.update { it.copy(timestamp = timestamp) }
    }

    fun saveTransaction() {
        val state = _uiState.value
        var hasError = false

        val amountInPaisa = CurrencyFormatter.parseRupeesToPaisa(state.amountText)
        if (amountInPaisa == null || amountInPaisa <= 0) {
            _uiState.update { it.copy(amountError = "Please enter a valid amount") }
            hasError = true
        }

        if (state.selectedCategory == null) {
            _uiState.update { it.copy(categoryError = "Please select a category") }
            hasError = true
        }

        if (hasError) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val transaction = TransactionEntity(
                type = state.transactionType,
                amount = amountInPaisa!!,
                categoryId = state.selectedCategory!!.id,
                description = state.description.ifBlank { null },
                timestamp = state.timestamp,
                paymentMethod = state.selectedPaymentMethod,
            )
            transactionRepository.insertTransaction(transaction)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(transactionRepository, categoryRepository) as T
        }
    }
}
