package com.sindantechnologies.gharhisab.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.util.Constants
import com.sindantechnologies.gharhisab.util.reminder.ReminderScheduler
import com.sindantechnologies.gharhisab.util.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * Handles creation, encryption, export, decryption, and atomic restoration
 * of GharHisab financial backups.
 */
class BackupManager(
    private val database: GharHisabDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context
) {

    /**
     * Gathers all data from database and DataStore, serializes to JSON,
     * and encrypts using AES-256-GCM with the user's password.
     */
    suspend fun createEncryptedBackupBytes(password: String): ByteArray = withContext(Dispatchers.IO) {
        val categories = database.categoryDao().getAllCategoriesList()
        val transactions = database.transactionDao().getAllTransactionsList()
        val budgets = database.budgetDao().getAllBudgetsList()
        val plannedExpenses = database.plannedExpenseDao().getAllPlannedExpensesList()

        val startingBalance = userPreferencesRepository.startingBalanceFlow.first()
        val protectedReserve = userPreferencesRepository.protectedReserveFlow.first()

        val payload = BackupPayload(
            backupVersion = 1,
            appVersion = Constants.APP_VERSION,
            createdAtMillis = System.currentTimeMillis(),
            startingBalancePaisa = startingBalance,
            protectedReservePaisa = protectedReserve,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            plannedExpenses = plannedExpenses
        )

        val jsonString = payload.toJsonString()
        SecurityUtils.encryptAesGcm(jsonString.toByteArray(Charsets.UTF_8), password)
    }

    /**
     * Decrypts and restores the complete database state in an atomic Room transaction.
     * Returns total number of transactions restored.
     */
    suspend fun restoreFromEncryptedBackupBytes(
        encryptedBytes: ByteArray,
        password: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val decryptedBytes = SecurityUtils.decryptAesGcm(encryptedBytes, password)
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val payload = BackupPayload.fromJsonString(jsonString)

            database.withTransaction {
                // 1. Restore categories
                if (payload.categories.isNotEmpty()) {
                    database.categoryDao().insertCategories(payload.categories)
                }

                // 2. Restore transactions
                database.transactionDao().deleteAllTransactions()
                if (payload.transactions.isNotEmpty()) {
                    database.transactionDao().insertTransactions(payload.transactions)
                }

                // 3. Restore budgets
                database.budgetDao().deleteAllBudgets()
                if (payload.budgets.isNotEmpty()) {
                    database.budgetDao().insertBudgets(payload.budgets)
                }

                // 4. Restore planned expenses
                database.plannedExpenseDao().deleteAllPlannedExpenses()
                if (payload.plannedExpenses.isNotEmpty()) {
                    database.plannedExpenseDao().insertPlannedExpenses(payload.plannedExpenses)
                }
            }

            // 5. Restore user preferences
            userPreferencesRepository.setStartingBalance(payload.startingBalancePaisa)
            userPreferencesRepository.setProtectedReserve(payload.protectedReservePaisa)

            // 6. Reschedule reminders for active future planned expenses
            val activeExpenses = database.plannedExpenseDao().getActivePlannedExpensesList()
            activeExpenses.forEach { expense ->
                ReminderScheduler.scheduleReminders(context, expense)
            }

            Result.success(payload.transactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports encrypted backup directly to an OutputStream (e.g. SAF file URI or Google Drive stream).
     */
    suspend fun exportToStream(outputStream: OutputStream, password: String) = withContext(Dispatchers.IO) {
        val encryptedBytes = createEncryptedBackupBytes(password)
        outputStream.use { out ->
            out.write(encryptedBytes)
            out.flush()
        }
    }

    /**
     * Imports and restores from an InputStream (e.g. SAF file URI or Google Drive stream).
     */
    suspend fun importFromStream(inputStream: InputStream, password: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val bytes = inputStream.use { it.readBytes() }
            restoreFromEncryptedBackupBytes(bytes, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
