package com.sindantechnologies.gharhisab.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.StorageOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Storage Selection screen.
 */
class StorageSelectionViewModel(
    private val repository: UserPreferencesRepository,
) : ViewModel() {

    private val _selectedStorage = MutableStateFlow(StorageOption.LOCAL)
    val selectedStorage: StateFlow<StorageOption> = _selectedStorage.asStateFlow()

    private val _showComingSoonDialog = MutableStateFlow(false)
    val showComingSoonDialog: StateFlow<Boolean> = _showComingSoonDialog.asStateFlow()

    /** Updates the selected storage option. */
    fun selectStorage(option: StorageOption) {
        _selectedStorage.value = option
    }

    /** Handles the Continue button press. */
    fun onContinue(navigateForward: () -> Unit) {
        if (_selectedStorage.value == StorageOption.GOOGLE_DRIVE) {
            _showComingSoonDialog.value = true
        } else {
            viewModelScope.launch {
                repository.setStorageOption(StorageOption.LOCAL)
                repository.setOnboardingComplete()
                navigateForward()
            }
        }
    }

    /** Continues with local storage from the Coming Soon dialog. */
    fun continueWithLocal() {
        _showComingSoonDialog.value = false
        _selectedStorage.value = StorageOption.LOCAL
        viewModelScope.launch {
            repository.setStorageOption(StorageOption.LOCAL)
            repository.setOnboardingComplete()
        }
    }

    /** Dismisses the Coming Soon dialog. */
    fun dismissDialog() {
        _showComingSoonDialog.value = false
    }

    /** Factory for manual dependency injection. */
    class Factory(
        private val repository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StorageSelectionViewModel(repository) as T
        }
    }
}
