package com.sindantechnologies.gharhisab.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND timestamp >= :startDate AND timestamp <= :endDate")
    fun getIncomeTotalBetween(startDate: Long, endDate: Long): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :startDate AND timestamp <= :endDate")
    fun getExpenseTotalBetween(startDate: Long, endDate: Long): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    fun getAllTimeIncomeTotal(): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getAllTimeExpenseTotal(): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND categoryId = :categoryId AND timestamp >= :startDate AND timestamp <= :endDate")
    fun getCategoryExpenseTotalBetween(categoryId: Long, startDate: Long, endDate: Long): Flow<Long?>

    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp >= :startDate AND timestamp <= :endDate
          AND (:type IS NULL OR type = :type)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:searchQuery = '' OR (description IS NOT NULL AND description LIKE '%' || :searchQuery || '%'))
        ORDER BY timestamp DESC
    """)
    fun filterTransactions(
        startDate: Long,
        endDate: Long,
        type: String?,
        categoryId: Long?,
        searchQuery: String
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
}
