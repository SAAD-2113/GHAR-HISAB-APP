package com.sindantechnologies.gharhisab.di

import android.content.Context
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.data.local.room.GharHisabDatabase
import com.sindantechnologies.gharhisab.data.repository.BudgetRepository
import com.sindantechnologies.gharhisab.data.repository.CategoryRepository
import com.sindantechnologies.gharhisab.data.repository.PlannedExpenseRepository
import com.sindantechnologies.gharhisab.data.repository.TransactionRepository

/**
 * Dependency Injection container.
 */
interface AppContainer {
    val context: Context
    val userPreferencesRepository: UserPreferencesRepository
    val transactionRepository: TransactionRepository
    val categoryRepository: CategoryRepository
    val budgetRepository: BudgetRepository
    val plannedExpenseRepository: PlannedExpenseRepository
}

/**
 * Implementation for the Dependency Injection container at the application level.
 */
class DefaultAppContainer(override val context: Context) : AppContainer {

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    private val database: GharHisabDatabase by lazy {
        GharHisabDatabase.getDatabase(context)
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }

    override val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(database.budgetDao())
    }

    override val plannedExpenseRepository: PlannedExpenseRepository by lazy {
        PlannedExpenseRepository(database.plannedExpenseDao(), database.transactionDao())
    }
}
