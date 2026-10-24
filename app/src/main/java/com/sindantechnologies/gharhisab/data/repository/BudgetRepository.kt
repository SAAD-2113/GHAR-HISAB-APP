package com.sindantechnologies.gharhisab.data.repository

import com.sindantechnologies.gharhisab.data.local.room.dao.BudgetDao
import com.sindantechnologies.gharhisab.data.local.room.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    fun getOverallBudget(month: Int, year: Int): Flow<BudgetEntity?> {
        return budgetDao.getOverallBudget(month, year)
    }

    fun getCategoryBudgets(month: Int, year: Int): Flow<List<BudgetEntity>> {
        return budgetDao.getCategoryBudgets(month, year)
    }

    fun getAllBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>> {
        return budgetDao.getAllBudgetsForMonth(month, year)
    }

    suspend fun getBudgetForCategory(month: Int, year: Int, categoryId: Long): BudgetEntity? {
        return budgetDao.getBudgetForCategory(month, year, categoryId)
    }

    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long {
        return budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao.deleteBudget(budget)
    }
}
