package com.sindantechnologies.gharhisab

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionPersistenceTest {
    @Test fun recordsAndTotalsSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "transaction-persistence-test"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, GharHisabDatabase::class.java, name)
            .addMigrations(GharHisabDatabase.MIGRATION_1_2).build()
        var db = open()
        try {
            db.categoryDao().insertCategories(listOf(
                CategoryEntity(1L, "Food", TransactionType.EXPENSE, "home"),
                CategoryEntity(2L, "Salary", TransactionType.INCOME, "payments"),
            ))
            db.transactionDao().insertTransaction(TransactionEntity(
                type = TransactionType.INCOME, amount = 100000L, categoryId = 2L,
                description = null, timestamp = 1000L, paymentMethod = null,
            ))
            val expenseId = db.transactionDao().insertTransaction(TransactionEntity(
                type = TransactionType.EXPENSE, amount = 25000L, categoryId = 1L,
                description = "Persist me", timestamp = 1000L, paymentMethod = null,
            ))
            val original = db.transactionDao().getTransactionById(expenseId)!!
            db.transactionDao().updateTransaction(original.copy(amount = 25550L))
            db.close()
            db = open()
            val restored = db.transactionDao().getTransactionById(expenseId)!!
            assertEquals("Persist me", restored.description)
            assertEquals(25550L, restored.amount)
            assertEquals(100000L, db.transactionDao().getIncomeTotalBetween(0, 2000).first())
            assertEquals(25550L, db.transactionDao().getExpenseTotalBetween(0, 2000).first())
            db.transactionDao().deleteTransaction(restored)
            assertNull(db.transactionDao().getTransactionById(expenseId))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
