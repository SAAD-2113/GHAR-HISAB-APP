package com.sindantechnologies.gharhisab

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.data.local.room.entity.*
import com.sindantechnologies.gharhisab.data.repository.PlannedExpenseRepository
import com.sindantechnologies.gharhisab.domain.model.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Version3DatabaseTest {
    private lateinit var db: GharHisabDatabase
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), GharHisabDatabase::class.java).build()
        db.categoryDao().insertCategories(listOf(CategoryEntity(1, "Bill", TransactionType.EXPENSE, "home")))
    }
    @After fun teardown() { db.close() }
    @Test fun concurrentPaymentsCreateOneTransactionAndOneNextPlan() = runBlocking {
        val repo = PlannedExpenseRepository(db)
        val date = LocalDate.of(2026,1,31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val id = repo.insertPlannedExpense(PlannedExpenseEntity(description = "Rent", expectedAmount = 10000, categoryId = 1, plannedDate = date, recurrence = "MONTHLY"))
        val results = (1..12).map { async(Dispatchers.Default) { repo.markAsPaid(id) } }.awaitAll()
        assertEquals(1, results.toSet().size)
        assertEquals(1, db.transactionDao().getAllTransactionsList().size)
        val plans = db.plannedExpenseDao().getAllPlannedExpensesList()
        assertEquals(2, plans.size)
        assertEquals(1, plans.count { it.status == PlannedExpenseStatus.PLANNED })
        assertEquals(LocalDate.of(2026,2,28), Instant.ofEpochMilli(plans.single { it.status == PlannedExpenseStatus.PLANNED }.plannedDate).atZone(ZoneId.systemDefault()).toLocalDate())
    }
    @Test fun cancelledPlanCannotCreatePayment() = runBlocking {
        val repo = PlannedExpenseRepository(db)
        val id = repo.insertPlannedExpense(PlannedExpenseEntity(description = "Bill", expectedAmount = 100, categoryId = 1, plannedDate = 1000))
        repo.cancelPlannedExpense(id)
        assertTrue(runCatching { repo.markAsPaid(id) }.isFailure)
        assertTrue(db.transactionDao().getAllTransactionsList().isEmpty())
    }
    @Test fun migrationFromFreshVersionTwoPreservesMoneyAndAddsDefaults() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-v2"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val old = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        old.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, transactionType TEXT NOT NULL, iconKey TEXT NOT NULL, isDefault INTEGER NOT NULL, isActive INTEGER NOT NULL)")
        old.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount INTEGER NOT NULL, categoryId INTEGER NOT NULL, description TEXT, timestamp INTEGER NOT NULL, paymentMethod TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        old.execSQL("CREATE INDEX index_transactions_categoryId ON transactions(categoryId)")
        old.execSQL("CREATE TABLE budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, categoryId INTEGER, month INTEGER NOT NULL, year INTEGER NOT NULL, amount INTEGER NOT NULL, warningThresholdPercent INTEGER NOT NULL)")
        old.execSQL("CREATE TABLE planned_expenses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, description TEXT NOT NULL, expectedAmount INTEGER NOT NULL, categoryId INTEGER NOT NULL, plannedDate INTEGER NOT NULL, notes TEXT, recurrence TEXT, status TEXT NOT NULL, actualTransactionId INTEGER, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        old.execSQL("CREATE INDEX index_planned_expenses_categoryId ON planned_expenses(categoryId)")
        old.execSQL("INSERT INTO categories VALUES (1,'Bill','EXPENSE','home',1,1)")
        old.execSQL("INSERT INTO transactions VALUES (1,'EXPENSE',12345,1,'Keep',1000,'CASH',1000,1000)")
        old.execSQL("INSERT INTO budgets VALUES (1,NULL,11,2026,100000,80)")
        old.execSQL("INSERT INTO planned_expenses VALUES (1,'Rent',25000,1,1000,NULL,'MONTHLY','PLANNED',NULL,1000,1000)")
        old.version = 2; old.close()
        val migrated = Room.databaseBuilder(context, GharHisabDatabase::class.java, name).addMigrations(GharHisabDatabase.MIGRATION_2_3).build()
        try {
            assertEquals(12345L, migrated.transactionDao().getTransactionById(1)!!.amount)
            assertEquals(100000L, migrated.budgetDao().getAllBudgetsList().single().amount)
            val plan = migrated.plannedExpenseDao().getPlannedExpenseById(1)!!
            assertEquals("0,1,3", plan.reminderDays); assertTrue(plan.remindersEnabled); assertEquals(1, plan.recurrenceInterval)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
