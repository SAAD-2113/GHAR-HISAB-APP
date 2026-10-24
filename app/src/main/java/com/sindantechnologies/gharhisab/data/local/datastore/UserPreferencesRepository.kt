package com.sindantechnologies.gharhisab.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sindantechnologies.gharhisab.domain.model.Language
import com.sindantechnologies.gharhisab.domain.model.StorageOption
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore singleton delegate. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ghar_hisab_preferences"
)

/**
 * Repository for all user preferences persisted via DataStore.
 *
 * Provides reactive [Flow] access and suspend functions for updates.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val STORAGE_OPTION = stringPreferencesKey("storage_option")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val STARTING_BALANCE = longPreferencesKey("starting_balance") // Paisa
        val PROTECTED_RESERVE = longPreferencesKey("protected_reserve") // Paisa
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language") // BCP-47 code
    }

    // ── Read Flows ──

    /** Emits the currently selected [Language]. */
    val languageFlow: Flow<Language> = context.dataStore.data.map { prefs ->
        Language.fromCode(prefs[Keys.LANGUAGE] ?: Language.ENGLISH.code)
    }

    /** Emits the currently selected [ThemeMode]. */
    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromKey(prefs[Keys.THEME_MODE] ?: ThemeMode.FOLLOW_SYSTEM.key)
    }

    /** Emits the currently selected [StorageOption]. */
    val storageOptionFlow: Flow<StorageOption> = context.dataStore.data.map { prefs ->
        StorageOption.fromKey(prefs[Keys.STORAGE_OPTION] ?: StorageOption.LOCAL.key)
    }

    /** Emits whether the onboarding flow has been completed. */
    val onboardingCompleteFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    /** Emits user-configured starting/current available balance in paisa (default 0L). */
    val startingBalanceFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.STARTING_BALANCE] ?: 0L
    }

    /** Emits user-configured emergency/protected reserve in paisa (default 0L). */
    val protectedReserveFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.PROTECTED_RESERVE] ?: 0L
    }

    /** Emits preferred voice recognition language code ("en", "ur", "pa"). */
    val voiceLanguageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.VOICE_LANGUAGE] ?: "en"
    }

    // ── Write Operations ──

    /** Persists the selected [language]. */
    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    /** Persists the selected [themeMode]. */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.key
        }
    }

    /** Persists the selected [storageOption]. */
    suspend fun setStorageOption(storageOption: StorageOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STORAGE_OPTION] = storageOption.key
        }
    }

    /** Marks onboarding as complete. */
    suspend fun setOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = true
        }
    }

    /** Persists starting balance in paisa. */
    suspend fun setStartingBalance(paisa: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STARTING_BALANCE] = maxOf(0L, paisa)
        }
    }

    /** Persists protected reserve in paisa. */
    suspend fun setProtectedReserve(paisa: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROTECTED_RESERVE] = maxOf(0L, paisa)
        }
    }

    /** Persists voice recognition language. */
    suspend fun setVoiceLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VOICE_LANGUAGE] = languageCode
        }
    }
}
