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

data class EditTransactionUiState(
    val transactionId: Long = 0L,
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
    val isDeleted: Boolean = false,
    val isLoading: Boolean = true,
)

class EditTransactionViewModel(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTransactionUiState(transactionId = transactionId))
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    private var originalTransaction: TransactionEntity? = null
    private var allCategories: List<CategoryEntity> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId)
            if (transaction == null) {
                _uiState.update { it.copy(isDeleted = true, isLoading = false) }
                return@launch
            }
            originalTransaction = transaction
            
            categoryRepository.getActiveCategories().collect { categories ->
                allCategories = categories
                
                val filteredCategories = allCategories.filter {
                    it.transactionType == transaction.type
                }
                
                val selectedCat = allCategories.find { it.id == transaction.categoryId }
                
                _uiState.update { 
                    it.copy(
                        transactionType = transaction.type,
                        amountText = CurrencyFormatter.formatPaisaRaw(transaction.amount),
                        description = transaction.description ?: "",
                        selectedCategory = selectedCat,
                        selectedPaymentMethod = transaction.paymentMethod ?: PaymentMethod.CASH,
                        timestamp = transaction.timestamp,
                        categories = filteredCategories,
                        isLoading = false
                    ) 
                }
            }
        }
    }

    fun onTransactionTypeChanged(type: TransactionType) {
        _uiState.update {
            it.copy(
                transactionType = type,
                selectedCategory = null,
                categoryError = null,
                categories = allCategories.filter { cat -> cat.transactionType == type }
            )
        }
    }

    fun onAmountChanged(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val dotCount = filtered.count { it == '.' }
        if (dotCount > 1) return
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
        val original = originalTransaction ?: return
        
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
            val transaction = original.copy(
                type = state.transactionType,
                amount = amountInPaisa!!,
                categoryId = state.selectedCategory!!.id,
                description = state.description.ifBlank { null },
                timestamp = state.timestamp,
                paymentMethod = state.selectedPaymentMethod,
                updatedAt = System.currentTimeMillis()
            )
            transactionRepository.updateTransaction(transaction)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
    
    fun deleteTransaction() {
        val original = originalTransaction ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            transactionRepository.deleteTransaction(original)
            _uiState.update { it.copy(isSaving = false, isDeleted = true) }
        }
    }

    class Factory(
        private val transactionId: Long,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditTransactionViewModel(transactionId, transactionRepository, categoryRepository) as T
        }
    }
}
