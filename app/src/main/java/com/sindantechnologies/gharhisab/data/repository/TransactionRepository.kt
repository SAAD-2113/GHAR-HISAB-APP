package com.sindantechnologies.gharhisab.data.repository

import com.sindantechnologies.gharhisab.data.local.room.dao.TransactionDao
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
    }

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> {
        return transactionDao.getRecentTransactions(limit)
    }

    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startDate, endDate)
    }

    fun getIncomeTotalBetween(startDate: Long, endDate: Long): Flow<Long?> {
        return transactionDao.getIncomeTotalBetween(startDate, endDate)
    }

    fun getExpenseTotalBetween(startDate: Long, endDate: Long): Flow<Long?> {
        return transactionDao.getExpenseTotalBetween(startDate, endDate)
    }

    fun getAllTimeIncomeTotal(): Flow<Long?> {
        return transactionDao.getAllTimeIncomeTotal()
    }

    fun getAllTimeExpenseTotal(): Flow<Long?> {
        return transactionDao.getAllTimeExpenseTotal()
    }

    fun getCategoryExpenseTotalBetween(categoryId: Long, startDate: Long, endDate: Long): Flow<Long?> {
        return transactionDao.getCategoryExpenseTotalBetween(categoryId, startDate, endDate)
    }

    fun filterTransactions(
        startDate: Long,
        endDate: Long,
        type: String? = null,
        categoryId: Long? = null,
        searchQuery: String = ""
    ): Flow<List<TransactionEntity>> {
        return transactionDao.filterTransactions(
            startDate = startDate,
            endDate = endDate,
            type = type,
            categoryId = categoryId,
            searchQuery = searchQuery.trim()
        )
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }
}
