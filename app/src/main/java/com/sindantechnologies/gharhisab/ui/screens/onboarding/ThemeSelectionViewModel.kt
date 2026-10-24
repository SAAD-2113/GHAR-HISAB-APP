package com.sindantechnologies.gharhisab.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Theme Selection screen.
 *
 * Theme changes are persisted immediately so the user sees the
 * live preview as they select Classic / Midnight / Follow System.
 */
class ThemeSelectionViewModel(
    private val repository: UserPreferencesRepository,
) : ViewModel() {

    private val _selectedTheme = MutableStateFlow(ThemeMode.FOLLOW_SYSTEM)
    val selectedTheme: StateFlow<ThemeMode> = _selectedTheme.asStateFlow()

    /** Updates the selected theme and persists immediately for live preview. */
    fun selectTheme(theme: ThemeMode) {
        _selectedTheme.value = theme
        // Persist immediately so the app-level theme observer picks up the change
        viewModelScope.launch {
            repository.setThemeMode(theme)
        }
    }

    /** Saves the final theme choice (already persisted via [selectTheme]). */
    fun saveTheme() {
        viewModelScope.launch {
            repository.setThemeMode(_selectedTheme.value)
        }
    }

    /** Factory for manual dependency injection. */
    class Factory(
        private val repository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThemeSelectionViewModel(repository) as T
        }
    }
}
