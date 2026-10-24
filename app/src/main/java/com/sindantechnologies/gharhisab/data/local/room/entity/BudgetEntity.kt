package com.sindantechnologies.gharhisab.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an overall monthly budget or category-specific budget.
 *
 * @param id Unique identifier.
 * @param categoryId Category ID if this is a category budget, or null if overall monthly budget.
 * @param month Month of the budget (1-12).
 * @param year Calendar year.
 * @param amount Budget amount stored as Long in paisa (1 PKR = 100 paisa).
 * @param warningThresholdPercent Spending percentage at which to trigger a warning (e.g. 80%).
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null,
    val month: Int,
    val year: Int,
    val amount: Long, // Paisa
    val warningThresholdPercent: Int = 80
)
