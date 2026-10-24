package com.sindantechnologies.gharhisab.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Language Selection screen.
 */
class LanguageSelectionViewModel(
    private val repository: UserPreferencesRepository,
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(Language.ENGLISH)
    val selectedLanguage: StateFlow<Language> = _selectedLanguage.asStateFlow()

    /** Updates the in-memory selected language. */
    fun selectLanguage(language: Language) {
        _selectedLanguage.value = language
    }

    /** Persists the selected language to DataStore. */
    fun saveLanguage() {
        viewModelScope.launch {
            repository.setLanguage(_selectedLanguage.value)
        }
    }

    /** Factory for manual dependency injection. */
    class Factory(
        private val repository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LanguageSelectionViewModel(repository) as T
        }
    }
}
