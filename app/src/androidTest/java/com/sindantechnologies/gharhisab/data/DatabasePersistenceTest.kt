package com.sindantechnologies.gharhisab.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.data.local.room.dao.CategoryDao
import com.sindantechnologies.gharhisab.data.local.room.dao.TransactionDao
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabasePersistenceTest {

    private lateinit var db: GharHisabDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database for testing, but in a real persistence test we'd use a real DB on disk.
        // To properly test persistence across process kills, we will use a physical db file and then close/reopen it.
        context.deleteDatabase("test_gharhisab_database")
        db = Room.databaseBuilder(
            context,
            GharHisabDatabase::class.java,
            "test_gharhisab_database"
        ).addMigrations(GharHisabDatabase.MIGRATION_1_2).build()
        transactionDao = db.transactionDao()
        categoryDao = db.categoryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun verifyTransactionIsSavedAndPersisted() = runBlocking {
        // 1. Setup a category
        val category = CategoryEntity(
            name = "Test Category",
            transactionType = TransactionType.EXPENSE,
            iconKey = "home"
        )
        categoryDao.insertCategories(listOf(category))
        
        val activeCategories = categoryDao.getActiveCategories().first()
        assertTrue(activeCategories.isNotEmpty())
        val savedCategoryId = activeCategories.first().id

        // 2. Create a transaction
        val amountInPaisa = 50000L // Rs. 500
        val transaction = TransactionEntity(
            type = TransactionType.EXPENSE,
            amount = amountInPaisa,
            categoryId = savedCategoryId,
            description = "Test Groceries",
            timestamp = System.currentTimeMillis(),
            paymentMethod = PaymentMethod.CASH
        )
        val transactionId = transactionDao.insertTransaction(transaction)

        // 3. Verify it is saved
        var retrieved = transactionDao.getTransactionById(transactionId)
        assertEquals("Test Groceries", retrieved?.description)
        assertEquals(50000L, retrieved?.amount)

        // 4. Simulate application close by closing the DB
        db.close()

        // 5. Re-open DB (simulating app restart)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.databaseBuilder(
            context,
            GharHisabDatabase::class.java,
            "test_gharhisab_database"
        ).addMigrations(GharHisabDatabase.MIGRATION_1_2).build()
        transactionDao = db.transactionDao()
        categoryDao = db.categoryDao()

        // 6. Verify the transaction is still present
        retrieved = transactionDao.getTransactionById(transactionId)
        assertEquals("Test Groceries", retrieved?.description)
        assertEquals(50000L, retrieved?.amount)

        // 7. Verify dashboard totals
        val start = 0L
        val end = System.currentTimeMillis() + 100000L
        val totalExpense = transactionDao.getExpenseTotalBetween(start, end).first()
        assertEquals(50000L, totalExpense)
    }
}
