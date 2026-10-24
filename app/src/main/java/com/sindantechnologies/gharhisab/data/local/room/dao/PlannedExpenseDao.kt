package com.sindantechnologies.gharhisab.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedExpenseDao {

    @Query("SELECT * FROM planned_expenses ORDER BY plannedDate ASC")
    fun getAllPlannedExpenses(): Flow<List<PlannedExpenseEntity>>

    @Query("SELECT * FROM planned_expenses WHERE status = 'PLANNED' ORDER BY plannedDate ASC")
    fun getUpcomingPlannedExpenses(): Flow<List<PlannedExpenseEntity>>

    @Query("SELECT * FROM planned_expenses WHERE plannedDate >= :startDate AND plannedDate <= :endDate ORDER BY plannedDate ASC")
    fun getPlannedExpensesBetween(startDate: Long, endDate: Long): Flow<List<PlannedExpenseEntity>>

    @Query("SELECT SUM(expectedAmount) FROM planned_expenses WHERE status = 'PLANNED' AND plannedDate >= :startDate AND plannedDate <= :endDate")
    fun getUnpaidPlannedTotalBetween(startDate: Long, endDate: Long): Flow<Long?>

    @Query("SELECT * FROM planned_expenses WHERE id = :id")
    suspend fun getPlannedExpenseById(id: Long): PlannedExpenseEntity?

    @Query("SELECT * FROM planned_expenses WHERE actualTransactionId = :transactionId LIMIT 1")
    suspend fun getPlannedExpenseByTransactionId(transactionId: Long): PlannedExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedExpense(plannedExpense: PlannedExpenseEntity): Long

    @Update
    suspend fun updatePlannedExpense(plannedExpense: PlannedExpenseEntity)

    @Delete
    suspend fun deletePlannedExpense(plannedExpense: PlannedExpenseEntity)
}
