package com.sindantechnologies.gharhisab.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.util.Constants
import com.sindantechnologies.gharhisab.util.reminder.ReminderScheduler
import com.sindantechnologies.gharhisab.util.security.SecurityUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

class BackupManager(
    private val database: GharHisabDatabase,
    private val preferences: UserPreferencesRepository,
    private val context: Context,
    private val vault: SafetyVault = KeystoreSafetyVault(context),
) {
    companion object { const val MAX_BYTES = 16 * 1024 * 1024 }
    data class Preview(val payload: BackupPayload, internal val localFingerprint: String)
    private val mutex = Mutex()
    private val recoveryBlockedState = kotlinx.coroutines.flow.MutableStateFlow(false)
    val recoveryBlocked: kotlinx.coroutines.flow.StateFlow<Boolean> = recoveryBlockedState
    private suspend fun snapshot(): BackupPayload = database.withTransaction {
        BackupPayload(appVersion = Constants.APP_VERSION, createdAtMillis = 1,
            startingBalancePaisa = preferences.startingBalanceFlow.first(), protectedReservePaisa = preferences.protectedReserveFlow.first(),
            language = preferences.languageFlow.first().code, theme = preferences.themeModeFlow.first().key,
            categories = database.categoryDao().getAllCategoriesList(), transactions = database.transactionDao().getAllTransactionsList(),
            budgets = database.budgetDao().getAllBudgetsList(), plannedExpenses = database.plannedExpenseDao().getAllPlannedExpensesList())
    }
    private fun fingerprint(payload: BackupPayload) = SecurityUtils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(payload.toJsonString().toByteArray()))
    suspend fun createEncryptedBackupBytes(password: String): ByteArray = withContext(Dispatchers.IO) {
        require(password.length >= 12) { "Use a recovery password with at least 12 characters" }
        val payload = snapshot().copy(createdAtMillis = System.currentTimeMillis())
        payload.validate()
        val data = payload.toJsonString().toByteArray(Charsets.UTF_8)
        require(data.size <= MAX_BYTES - 64)
        SecurityUtils.encryptAesGcm(data, password)
    }
    suspend fun preview(bytes: ByteArray, password: String): Preview = withContext(Dispatchers.IO) {
        require(bytes.size <= MAX_BYTES)
        val payload = BackupPayload.fromJsonString(String(SecurityUtils.decryptAesGcm(bytes, password), Charsets.UTF_8))
        Preview(payload, fingerprint(snapshot()))
    }
    private suspend fun apply(payload: BackupPayload) {
        payload.validate()
        database.withTransaction {
            database.transactionDao().deleteAllTransactions()
            database.plannedExpenseDao().deleteAllPlannedExpenses()
            database.budgetDao().deleteAllBudgets()
            database.categoryDao().deleteAllCategories()
            database.categoryDao().insertCategories(payload.categories)
            database.transactionDao().insertTransactions(payload.transactions)
            database.budgetDao().insertBudgets(payload.budgets)
            database.plannedExpenseDao().insertPlannedExpenses(payload.plannedExpenses)
            preferences.restoreFinancialPreferences(payload.startingBalancePaisa, payload.protectedReservePaisa, payload.language, payload.theme)
        }
    }
    /** Must complete before financial UI is displayed on startup. A pending journal means rollback. */
    suspend fun recoverInterruptedRestore() = withContext(Dispatchers.IO) {
        mutex.withLock {
            vault.readPending()?.let {
                apply(BackupPayload.fromJsonString(String(it, Charsets.UTF_8)))
                vault.complete()
            }
        }
    }
    suspend fun restore(preview: Preview): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            withContext(NonCancellable) {
                check(!recoveryBlockedState.value) { "Restart to recover the interrupted restore" }
                var before: BackupPayload? = null
                try {
                    database.withTransaction {
                        val current = snapshot()
                        check(fingerprint(current) == preview.localFingerprint) { "Local data changed; preview again before replacing it" }
                        vault.writePending(current.toJsonString().toByteArray())
                        before = current
                        apply(preview.payload)
                    }
                    // Until this succeeds, startup recovery deliberately restores the previous state.
                    vault.complete()
                } catch (error: Exception) {
                    before?.let { original ->
                        try {
                            apply(original)
                            vault.complete()
                        } catch (rollbackError: Exception) {
                            // Do not allow edits that a pending startup journal could subsequently discard.
                            recoveryBlockedState.value = true
                            error.addSuppressed(rollbackError)
                        }
                    }
                    throw error
                }
                // The data restore is already committed. Android can reject alarm scheduling;
                // retry at the next foreground entry instead of reporting the data restore failed.
                runCatching { ReminderScheduler.reconcile(context, database.plannedExpenseDao().getAllPlannedExpensesList()) }
                preview.payload.transactions.size
            }
        }
    }
    suspend fun rollbackSafetyBackup(): Int = withContext(Dispatchers.IO) {
        val bytes = vault.readSafety() ?: error("No local safety backup")
        restore(Preview(BackupPayload.fromJsonString(String(bytes, Charsets.UTF_8)), fingerprint(snapshot())))
    }
    suspend fun exportToStream(output: OutputStream, password: String) = withContext(Dispatchers.IO) {
        val bytes = createEncryptedBackupBytes(password)
        output.use { it.write(bytes); it.flush() }
    }
    suspend fun readBounded(input: InputStream): ByteArray = withContext(Dispatchers.IO) {
        input.use {
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                require(out.size() + count <= MAX_BYTES) { "Backup is too large" }
                out.write(buffer, 0, count)
            }
            out.toByteArray()
        }
    }
}
