package com.sindantechnologies.gharhisab.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus

/**
 * Entity representing a planned future expense commitment.
 *
 * Critical Rule: A planned expense must NOT be counted as an actual expense
 * until it is recorded as paid and converted into an actual transaction.
 *
 * @param id Unique identifier.
 * @param description Description of the planned expense.
 * @param expectedAmount Expected amount stored as Long in paisa (1 PKR = 100 paisa).
 * @param categoryId Associated category ID.
 * @param plannedDate Scheduled date timestamp (in milliseconds).
 * @param notes Optional user notes.
 * @param recurrence Optional recurrence frequency ("NONE", "WEEKLY", "MONTHLY").
 * @param status Status of the plan (PLANNED, PAID, CANCELLED).
 * @param actualTransactionId If marked as paid, stores the created TransactionEntity ID to guarantee idempotency.
 * @param createdAt Creation timestamp.
 * @param updatedAt Last update timestamp.
 */
@Entity(
    tableName = "planned_expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class PlannedExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val expectedAmount: Long, // Paisa
    val categoryId: Long,
    val plannedDate: Long, // Epoch ms
    val notes: String? = null,
    val recurrence: String? = null,
    val status: PlannedExpenseStatus = PlannedExpenseStatus.PLANNED,
    val actualTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
