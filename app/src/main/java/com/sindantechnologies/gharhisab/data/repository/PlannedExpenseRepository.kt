package com.sindantechnologies.gharhisab.data.repository

import com.sindantechnologies.gharhisab.data.local.room.dao.PlannedExpenseDao
import com.sindantechnologies.gharhisab.data.local.room.dao.TransactionDao
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.domain.model.Recurrence

class PlannedExpenseRepository(
    private val database: GharHisabDatabase,
    private val onChanged: suspend () -> Unit = {},
) {
    private val plannedExpenseDao = database.plannedExpenseDao()
    private val transactionDao = database.transactionDao()

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

    suspend fun getActivePlannedExpensesList(currentTimeMillis: Long = System.currentTimeMillis()): List<PlannedExpenseEntity> {
        return plannedExpenseDao.getActivePlannedExpensesList(currentTimeMillis)
    }

    suspend fun insertPlannedExpense(item: PlannedExpenseEntity): Long {
        validate(item)
        val id = plannedExpenseDao.insertPlannedExpense(item)
        onChanged()
        return id
    }

    suspend fun updatePlannedExpense(item: PlannedExpenseEntity) {
        validate(item)
        database.withTransaction {
            val current = plannedExpenseDao.getPlannedExpenseById(item.id) ?: error("Plan no longer exists")
            check(current.status == PlannedExpenseStatus.PLANNED) { "Only unpaid plans can be edited" }
            plannedExpenseDao.updatePlannedExpense(item.copy(status = current.status, actualTransactionId = current.actualTransactionId))
        }
        onChanged()
    }

    suspend fun deletePlannedExpense(item: PlannedExpenseEntity) {
        plannedExpenseDao.deletePlannedExpense(item)
        onChanged()
    }

    suspend fun cancelPlannedExpense(id: Long) {
        database.withTransaction {
            val existing = plannedExpenseDao.getPlannedExpenseById(id) ?: return@withTransaction
            if (existing.status == PlannedExpenseStatus.PLANNED) {
                plannedExpenseDao.updatePlannedExpense(existing.copy(status = PlannedExpenseStatus.CANCELLED, updatedAt = System.currentTimeMillis()))
            }
        }
        onChanged()
    }

    private suspend fun validate(item: PlannedExpenseEntity) {
        require(database.categoryDao().getCategoryById(item.categoryId)?.transactionType == TransactionType.EXPENSE)
        require(item.description.isNotBlank() && item.expectedAmount > 0 && item.plannedDate > 0)
        require(item.recurrenceInterval in 1..120)
        require(item.recurrence in listOf(null, "NONE", "WEEKLY", "MONTHLY", "YEARLY"))
        require(item.reminderDays.split(',').all { it.toIntOrNull() in listOf(0, 1, 3, 7) })
        require(item.recurrenceAnchorDay in 0..31)
    }

    /**
     * Converts a planned expense into an actual transaction exactly once.
     * Guarantees idempotency — will not create duplicate transactions if called repeatedly.
     */
    suspend fun markAsPaid(
        plannedExpenseId: Long,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        timestamp: Long = System.currentTimeMillis(),
        actualAmount: Long? = null
    ): Long? {
        val result = database.withTransaction {
            val existing = plannedExpenseDao.getPlannedExpenseById(plannedExpenseId) ?: return@withTransaction null
            if (existing.status == PlannedExpenseStatus.PAID) return@withTransaction existing.actualTransactionId
            check(existing.status == PlannedExpenseStatus.PLANNED) { "Cancelled plans cannot be paid" }
            validate(existing)
            require(actualAmount == null || actualAmount > 0)
            val transactionId = transactionDao.insertTransaction(TransactionEntity(
                type = TransactionType.EXPENSE, amount = actualAmount ?: existing.expectedAmount,
                categoryId = existing.categoryId, description = existing.description,
                timestamp = timestamp, paymentMethod = paymentMethod, plannedExpenseId = existing.id,
            ))
            plannedExpenseDao.updatePlannedExpense(existing.copy(
                status = PlannedExpenseStatus.PAID, actualTransactionId = transactionId,
                updatedAt = System.currentTimeMillis(),
            ))
            Recurrence.next(existing)?.let { next ->
                plannedExpenseDao.insertPlannedExpense(existing.copy(
                    id = 0, plannedDate = next, actualTransactionId = null,
                    recurrenceAnchorDay = Recurrence.anchorDay(existing),
                    createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(),
                ))
            }
            transactionId
        }
        onChanged()
        return result
    }
}
