package com.sindantechnologies.gharhisab.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sindantechnologies.gharhisab.domain.model.AppLockMode
import com.sindantechnologies.gharhisab.domain.model.AppLockTimeout
import com.sindantechnologies.gharhisab.domain.model.Language
import com.sindantechnologies.gharhisab.domain.model.StorageOption
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.util.security.SecurityUtils
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
class UserPreferencesRepository(
    private val context: Context,
    private val pinVerifier: com.sindantechnologies.gharhisab.util.security.PinVerifier = com.sindantechnologies.gharhisab.util.security.KeystorePinVerifier(),
) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val STORAGE_OPTION = stringPreferencesKey("storage_option")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val STARTING_BALANCE = longPreferencesKey("starting_balance") // Paisa
        val PROTECTED_RESERVE = longPreferencesKey("protected_reserve") // Paisa
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language") // BCP-47 code
        val APP_LOCK_MODE = stringPreferencesKey("app_lock_mode")
        val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        val APP_LOCK_PIN_SALT = stringPreferencesKey("app_lock_pin_salt")
        val ATTEMPTS = longPreferencesKey("lock_attempts")
        val BLOCKED_UNTIL = longPreferencesKey("lock_blocked_until")
        val BLOCKED_ELAPSED = longPreferencesKey("lock_blocked_elapsed")
        val BLOCKED_BOOT = longPreferencesKey("lock_blocked_boot")
        val RECOVERY_HASH = stringPreferencesKey("lock_recovery_hash")
        val APP_LOCK_TIMEOUT = longPreferencesKey("app_lock_timeout")
    }

    private val securityMutex = kotlinx.coroutines.sync.Mutex()
    data class SecurityState(val mode: AppLockMode, val salt: String, val hash: String, val timeout: Long, val attempts: Long, val blockedUntil: Long)
    val securityState = context.dataStore.data.map { p -> SecurityState(
        AppLockMode.fromKey(p[Keys.APP_LOCK_MODE] ?: AppLockMode.OFF.key), p[Keys.APP_LOCK_PIN_SALT].orEmpty(),
        p[Keys.APP_LOCK_PIN_HASH].orEmpty(), p[Keys.APP_LOCK_TIMEOUT] ?: 0, p[Keys.ATTEMPTS] ?: 0, p[Keys.BLOCKED_UNTIL] ?: 0,
    ) }

    suspend fun verifyAppPin(pin: String, recovery: Boolean = false): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        securityMutex.lock()
        try {
            var success = false
            val candidate = if (recovery) pin else SecurityUtils.normalizePinDigits(pin)
            context.dataStore.edit { p ->
                val now = System.currentTimeMillis()
                val elapsed = android.os.SystemClock.elapsedRealtime()
                val boot = android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, 0).toLong()
                val previousAttempts = p[Keys.ATTEMPTS] ?: 0
                if (previousAttempts >= 5 && p[Keys.BLOCKED_BOOT] != boot) {
                    p[Keys.BLOCKED_BOOT] = boot
                    p[Keys.BLOCKED_ELAPSED] = elapsed + 30_000L
                    return@edit
                }
                if (elapsed < (p[Keys.BLOCKED_ELAPSED] ?: 0)) return@edit
                val salt = p[Keys.APP_LOCK_PIN_SALT].orEmpty()
                val hash = p[Keys.APP_LOCK_PIN_HASH].orEmpty()
                success = if (recovery) SecurityUtils.verifyRecoveryKey(candidate, salt, p[Keys.RECOVERY_HASH].orEmpty())
                    else if (hash.startsWith(com.sindantechnologies.gharhisab.util.security.KeystorePinVerifier.PREFIX))
                        runCatching { pinVerifier.matches(candidate, SecurityUtils.hexToBytes(salt), hash) }.getOrDefault(false)
                    else SecurityUtils.verifyPin(candidate, salt, hash)
                if (success && !recovery && !hash.startsWith(com.sindantechnologies.gharhisab.util.security.KeystorePinVerifier.PREFIX)) {
                    // A legacy PIN pays the old PBKDF cost once, then uses the device-bound verifier.
                    // If Keystore is unavailable, keep the working legacy verifier rather than lose access.
                    runCatching { pinVerifier.create(candidate, SecurityUtils.hexToBytes(salt)) }.getOrNull()?.let { p[Keys.APP_LOCK_PIN_HASH] = it }
                }
                if (success) {
                    p[Keys.ATTEMPTS] = 0
                    p[Keys.BLOCKED_UNTIL] = 0
                    p[Keys.BLOCKED_ELAPSED] = 0
                    if (recovery) { p[Keys.APP_LOCK_MODE] = AppLockMode.OFF.key; p.remove(Keys.RECOVERY_HASH); p.remove(Keys.APP_LOCK_PIN_HASH); p.remove(Keys.APP_LOCK_PIN_SALT) }
                } else {
                    val attempts = (p[Keys.ATTEMPTS] ?: 0) + 1
                    p[Keys.ATTEMPTS] = attempts
                    if (attempts >= 5) {
                        val delay = minOf(3_600_000L, 30_000L * (1L shl minOf(7, ((attempts - 5) / 5).toInt())))
                        p[Keys.BLOCKED_UNTIL] = now + delay
                        p[Keys.BLOCKED_ELAPSED] = elapsed + delay
                        p[Keys.BLOCKED_BOOT] = boot
                    }
                }
            }
            success
        } finally { securityMutex.unlock() }
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

    /** Emits configured app security mode. */
    val appLockModeFlow: Flow<AppLockMode> = context.dataStore.data.map { prefs ->
        AppLockMode.fromKey(prefs[Keys.APP_LOCK_MODE] ?: AppLockMode.OFF.key)
    }

    /** Emits stored PIN hash. */
    val appLockPinHashFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_LOCK_PIN_HASH] ?: ""
    }

    /** Emits stored PIN salt. */
    val appLockPinSaltFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_LOCK_PIN_SALT] ?: ""
    }

    /** Emits configured auto-lock timeout duration in milliseconds. */
    val appLockTimeoutFlow: Flow<AppLockTimeout> = context.dataStore.data.map { prefs ->
        AppLockTimeout.fromDuration(prefs[Keys.APP_LOCK_TIMEOUT] ?: 0L)
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

    suspend fun restoreFinancialPreferences(balance: Long, reserve: Long, language: String, theme: String) {
        require(balance >= 0 && reserve >= 0)
        context.dataStore.edit { p ->
            p[Keys.STARTING_BALANCE] = balance; p[Keys.PROTECTED_RESERVE] = reserve
            p[Keys.LANGUAGE] = Language.fromCode(language).code
            p[Keys.THEME_MODE] = ThemeMode.fromKey(theme).key
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

    /** Persists the chosen app lock mode. */
    suspend fun setAppLockMode(mode: AppLockMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_MODE] = mode.key
        }
    }

    /** Securely hashes and persists a new user PIN. */
    suspend fun setAppLockPin(pin: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val normalizedPin = SecurityUtils.normalizePinDigits(pin)
        require(normalizedPin.length == 4 && normalizedPin.all { it in '0'..'9' })
        securityMutex.lock()
        try {
            val recovery = SecurityUtils.bytesToHex(SecurityUtils.generateSalt(16))
            val salt = SecurityUtils.generateSalt()
            // Keystore work is off the UI thread, and no expensive hash blocks DataStore readers.
            val hash = pinVerifier.create(normalizedPin, salt)
            val recoveryHash = SecurityUtils.hashRecoveryKey(recovery, salt)
            context.dataStore.edit { prefs ->
                prefs[Keys.APP_LOCK_PIN_SALT] = SecurityUtils.bytesToHex(salt)
                prefs[Keys.APP_LOCK_PIN_HASH] = hash
                prefs[Keys.APP_LOCK_MODE] = AppLockMode.PIN.key
                prefs[Keys.RECOVERY_HASH] = recoveryHash
                prefs[Keys.ATTEMPTS] = 0
                prefs[Keys.BLOCKED_UNTIL] = 0
                prefs[Keys.BLOCKED_ELAPSED] = 0
            }
            recovery
        } finally { securityMutex.unlock() }
    }

    /** Disables app lock and clears security credentials. */
    suspend fun clearAppLock() {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_MODE] = AppLockMode.OFF.key
            prefs[Keys.APP_LOCK_PIN_SALT] = ""
            prefs[Keys.APP_LOCK_PIN_HASH] = ""
            prefs.remove(Keys.RECOVERY_HASH)
            prefs[Keys.ATTEMPTS] = 0
            prefs[Keys.BLOCKED_ELAPSED] = 0
            prefs[Keys.BLOCKED_UNTIL] = 0
        }
    }

    /** Persists auto-lock timeout. */
    suspend fun setAppLockTimeout(timeout: AppLockTimeout) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_TIMEOUT] = timeout.durationMillis
        }
    }
}
