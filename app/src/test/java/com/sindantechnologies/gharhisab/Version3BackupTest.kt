package com.sindantechnologies.gharhisab

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.data.backup.*
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.data.local.room.entity.*
import com.sindantechnologies.gharhisab.domain.model.*
import com.sindantechnologies.gharhisab.util.security.SecurityUtils
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Version3BackupTest {
    private lateinit var db: GharHisabDatabase
    private lateinit var manager: BackupManager
    private val vault = object : SafetyVault {
        var pending: ByteArray? = null
        var safety: ByteArray? = null
        var failuresRemaining = 0
        override fun writePending(bytes: ByteArray) { pending = bytes }
        override fun readPending() = pending
        override fun readSafety() = safety
        override fun complete() {
            if (failuresRemaining > 0) { failuresRemaining--; error("Simulated storage failure") }
            safety = pending; pending = null
        }
    }
    private val password = "Correct Recovery Password 2026"
    @Before fun setup(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GharHisabDatabase::class.java).build()
        manager = BackupManager(db, UserPreferencesRepository(context), context, vault)
        db.categoryDao().insertCategories(listOf(CategoryEntity(1, "Food", TransactionType.EXPENSE, "home")))
        db.transactionDao().insertTransaction(TransactionEntity(1, TransactionType.EXPENSE, 12345, 1, "Private grocery description", 1000, PaymentMethod.CASH))
    }
    @After fun close() { db.close() }
    @Test fun portableEncryptionPreviewRestoreAndRollback() = runBlocking {
        val bytes = manager.createEncryptedBackupBytes(password)
        assertFalse(String(bytes).contains("Private grocery description"))
        db.transactionDao().updateTransaction(db.transactionDao().getTransactionById(1)!!.copy(amount = 999))
        val preview = manager.preview(bytes, password)
        assertEquals(12345L, preview.payload.transactions.single().amount)
        assertEquals(1, manager.restore(preview))
        assertEquals(12345L, db.transactionDao().getTransactionById(1)!!.amount)
        manager.rollbackSafetyBackup()
        assertEquals(999L, db.transactionDao().getTransactionById(1)!!.amount)
    }
    @Test fun concurrentLocalEditRejectsStalePreview() = runBlocking {
        val preview = manager.preview(manager.createEncryptedBackupBytes(password), password)
        db.transactionDao().updateTransaction(db.transactionDao().getTransactionById(1)!!.copy(amount = 999))
        assertTrue(runCatching { manager.restore(preview) }.isFailure)
        assertEquals(999L, db.transactionDao().getTransactionById(1)!!.amount)
    }
    @Test fun corruptedWrongPasswordAndUnknownSchemaNeverReplaceRecords() = runBlocking {
        val bytes = manager.createEncryptedBackupBytes(password)
        assertTrue(runCatching { manager.preview(bytes, "incorrect password") }.isFailure)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertTrue(runCatching { manager.preview(bytes, password) }.isFailure)
        val invalid = SecurityUtils.encryptAesGcm("{\"backupVersion\":99}".toByteArray(), password)
        assertTrue(runCatching { manager.preview(invalid, password) }.isFailure)
        assertEquals(12345L, db.transactionDao().getTransactionById(1)!!.amount)
    }
    @Test fun failedJournalFinalizationRollsBackWithoutClaimingSuccess() = runBlocking {
        val bytes = manager.createEncryptedBackupBytes(password)
        db.transactionDao().updateTransaction(db.transactionDao().getTransactionById(1)!!.copy(amount = 999))
        val preview = manager.preview(bytes, password)
        vault.failuresRemaining = 1
        assertTrue(runCatching { manager.restore(preview) }.isFailure)
        assertEquals(999L, db.transactionDao().getTransactionById(1)!!.amount)
        assertNull(vault.pending)
        assertFalse(manager.recoveryBlocked.value)
    }
    @Test fun persistentStorageFailureBlocksEditsUntilStartupRecovery() = runBlocking {
        val bytes = manager.createEncryptedBackupBytes(password)
        db.transactionDao().updateTransaction(db.transactionDao().getTransactionById(1)!!.copy(amount = 999))
        val preview = manager.preview(bytes, password)
        vault.failuresRemaining = 2
        assertTrue(runCatching { manager.restore(preview) }.isFailure)
        assertEquals(999L, db.transactionDao().getTransactionById(1)!!.amount)
        assertTrue(manager.recoveryBlocked.value)
        assertNotNull(vault.pending)
    }
    @Test fun interruptedRestoreRecoversOldRecordsBeforeUse() = runBlocking {
        val snapshot = manager.preview(manager.createEncryptedBackupBytes(password), password).payload
        vault.writePending(snapshot.toJsonString().toByteArray())
        db.transactionDao().deleteAllTransactions()
        manager.recoverInterruptedRestore()
        assertEquals(12345L, db.transactionDao().getTransactionById(1)!!.amount)
        assertNull(vault.pending)
    }
}
