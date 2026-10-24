package com.sindantechnologies.gharhisab.data.repository

import com.sindantechnologies.gharhisab.data.local.room.dao.PlannedExpenseDao
import com.sindantechnologies.gharhisab.data.local.room.dao.TransactionDao
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

class PlannedExpenseRepository(
    private val plannedExpenseDao: PlannedExpenseDao,
    private val transactionDao: TransactionDao,
) {

    fun getAllPlannedExpenses(): Flow<List<PlannedExpenseEntity>> {
        return plannedExpenseDao.getAllPlannedExpenses()
    }

    fun getUpcomingPlannedExpenses(): Flow<List<PlannedExpenseEntity>> {
        return plannedExpenseDao.getUpcomingPlannedExpenses()
    }

    fun getPlannedExpensesBetween(startDate: Long, endDate: Long): Flow<List<PlannedExpenseEntity>> {
        return plannedExpenseDao.getPlannedExpensesBetween(startDate, endDate)
    }

    fun getUnpaidPlannedTotalBetween(startDate: Long, endDate: Long): Flow<Long?> {
        return plannedExpenseDao.getUnpaidPlannedTotalBetween(startDate, endDate)
    }

    suspend fun getPlannedExpenseById(id: Long): PlannedExpenseEntity? {
        return plannedExpenseDao.getPlannedExpenseById(id)
    }

    suspend fun insertPlannedExpense(item: PlannedExpenseEntity): Long {
        return plannedExpenseDao.insertPlannedExpense(item)
    }

    suspend fun updatePlannedExpense(item: PlannedExpenseEntity) {
        plannedExpenseDao.updatePlannedExpense(item)
    }

    suspend fun deletePlannedExpense(item: PlannedExpenseEntity) {
        plannedExpenseDao.deletePlannedExpense(item)
    }

    suspend fun cancelPlannedExpense(id: Long) {
        val existing = plannedExpenseDao.getPlannedExpenseById(id) ?: return
        plannedExpenseDao.updatePlannedExpense(
            existing.copy(
                status = PlannedExpenseStatus.CANCELLED,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Converts a planned expense into an actual transaction exactly once.
     * Guarantees idempotency — will not create duplicate transactions if called repeatedly.
     */
    suspend fun markAsPaid(
        plannedExpenseId: Long,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        timestamp: Long = System.currentTimeMillis()
    ): Long? {
        val existing = plannedExpenseDao.getPlannedExpenseById(plannedExpenseId) ?: return null

        // Guard: Prevent duplicate actual transactions if already paid
        if (existing.status == PlannedExpenseStatus.PAID && existing.actualTransactionId != null) {
            return existing.actualTransactionId
        }

        // 1. Create real transaction
        val actualTransaction = TransactionEntity(
            type = TransactionType.EXPENSE,
            amount = existing.expectedAmount,
            categoryId = existing.categoryId,
            description = existing.description,
            timestamp = timestamp,
            paymentMethod = paymentMethod,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val transactionId = transactionDao.insertTransaction(actualTransaction)

        // 2. Mark planned expense as PAID and store reference to transaction
        val updatedPlan = existing.copy(
            status = PlannedExpenseStatus.PAID,
            actualTransactionId = transactionId,
            updatedAt = System.currentTimeMillis()
        )
        plannedExpenseDao.updatePlannedExpense(updatedPlan)

        return transactionId
    }
}
