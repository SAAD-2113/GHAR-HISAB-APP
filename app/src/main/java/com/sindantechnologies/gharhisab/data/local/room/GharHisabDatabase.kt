package com.sindantechnologies.gharhisab.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sindantechnologies.gharhisab.data.local.room.dao.BudgetDao
import com.sindantechnologies.gharhisab.data.local.room.dao.CategoryDao
import com.sindantechnologies.gharhisab.data.local.room.dao.PlannedExpenseDao
import com.sindantechnologies.gharhisab.data.local.room.dao.TransactionDao
import com.sindantechnologies.gharhisab.data.local.room.entity.BudgetEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        PlannedExpenseEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GharHisabDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun plannedExpenseDao(): PlannedExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: GharHisabDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `warningThresholdPercent` INTEGER NOT NULL DEFAULT 80
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `planned_expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `description` TEXT NOT NULL,
                        `expectedAmount` INTEGER NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `plannedDate` INTEGER NOT NULL,
                        `notes` TEXT,
                        `recurrence` TEXT,
                        `status` TEXT NOT NULL,
                        `actualTransactionId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_planned_expenses_categoryId` 
                    ON `planned_expenses` (`categoryId`)
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN plannedExpenseId INTEGER DEFAULT NULL")
                db.execSQL("UPDATE transactions SET plannedExpenseId = (SELECT id FROM planned_expenses WHERE actualTransactionId = transactions.id LIMIT 1)")
                // Normalize the v1-migrated and fresh-v2 budget defaults without discarding rows.
                db.execSQL("CREATE TABLE budgets_v3 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, categoryId INTEGER, month INTEGER NOT NULL, year INTEGER NOT NULL, amount INTEGER NOT NULL, warningThresholdPercent INTEGER NOT NULL DEFAULT 80)")
                db.execSQL("INSERT INTO budgets_v3 SELECT id, categoryId, month, year, amount, warningThresholdPercent FROM budgets")
                db.execSQL("DROP TABLE budgets")
                db.execSQL("ALTER TABLE budgets_v3 RENAME TO budgets")
                db.execSQL("ALTER TABLE planned_expenses ADD COLUMN remindersEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE planned_expenses ADD COLUMN reminderDays TEXT NOT NULL DEFAULT '0,1,3'")
                db.execSQL("ALTER TABLE planned_expenses ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE planned_expenses ADD COLUMN recurrenceAnchorDay INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): GharHisabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GharHisabDatabase::class.java,
                    "gharhisab_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.categoryDao())
                }
            }
        }

        private suspend fun populateInitialData(categoryDao: CategoryDao) {
            val initialCategories = listOf(
                // Expense Categories
                CategoryEntity(name = "Groceries", transactionType = TransactionType.EXPENSE, iconKey = "shopping_cart"),
                CategoryEntity(name = "Utilities", transactionType = TransactionType.EXPENSE, iconKey = "bolt"),
                CategoryEntity(name = "Transport", transactionType = TransactionType.EXPENSE, iconKey = "directions_car"),
                CategoryEntity(name = "Education", transactionType = TransactionType.EXPENSE, iconKey = "school"),
                CategoryEntity(name = "Health", transactionType = TransactionType.EXPENSE, iconKey = "medical_services"),
                CategoryEntity(name = "Rent", transactionType = TransactionType.EXPENSE, iconKey = "home"),
                CategoryEntity(name = "Dining", transactionType = TransactionType.EXPENSE, iconKey = "restaurant"),
                CategoryEntity(name = "Shopping", transactionType = TransactionType.EXPENSE, iconKey = "local_mall"),
                CategoryEntity(name = "Personal", transactionType = TransactionType.EXPENSE, iconKey = "person"),
                CategoryEntity(name = "Other", transactionType = TransactionType.EXPENSE, iconKey = "more_horiz"),
                
                // Income Categories
                CategoryEntity(name = "Salary", transactionType = TransactionType.INCOME, iconKey = "payments"),
                CategoryEntity(name = "Business", transactionType = TransactionType.INCOME, iconKey = "store"),
                CategoryEntity(name = "Rent Received", transactionType = TransactionType.INCOME, iconKey = "real_estate_agent"),
                CategoryEntity(name = "Freelance", transactionType = TransactionType.INCOME, iconKey = "work"),
                CategoryEntity(name = "Gift", transactionType = TransactionType.INCOME, iconKey = "card_giftcard"),
                CategoryEntity(name = "Other", transactionType = TransactionType.INCOME, iconKey = "more_horiz")
            )
            categoryDao.insertCategories(initialCategories)
        }
    }
}
