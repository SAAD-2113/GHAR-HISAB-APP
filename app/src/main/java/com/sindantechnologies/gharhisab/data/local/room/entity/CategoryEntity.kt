package com.sindantechnologies.gharhisab.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sindantechnologies.gharhisab.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val transactionType: TransactionType,
    val iconKey: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
