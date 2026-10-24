package com.sindantechnologies.gharhisab.data.local.room

import androidx.room.TypeConverter
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import com.sindantechnologies.gharhisab.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? {
        return value?.let { PaymentMethod.valueOf(it) }
    }

    @TypeConverter
    fun fromPlannedExpenseStatus(value: PlannedExpenseStatus): String {
        return value.name
    }

    @TypeConverter
    fun toPlannedExpenseStatus(value: String): PlannedExpenseStatus {
        return PlannedExpenseStatus.valueOf(value)
    }
}
