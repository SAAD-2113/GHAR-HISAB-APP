package com.sindantechnologies.gharhisab.data.repository

import com.sindantechnologies.gharhisab.data.local.room.dao.CategoryDao
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getActiveCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getActiveCategories()
    }

    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(type)
    }

    suspend fun getCategoryById(id: Long): CategoryEntity? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun getActiveCategoriesList(): List<CategoryEntity> {
        return categoryDao.getActiveCategoriesList()
    }
}
