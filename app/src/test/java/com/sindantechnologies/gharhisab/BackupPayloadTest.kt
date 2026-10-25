package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.data.backup.BackupPayload
import com.sindantechnologies.gharhisab.data.local.room.entity.BudgetEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupPayloadTest {

    @Test
    fun testBackupPayloadSerializationAndDeserialization() {
        val originalCategories = listOf(
            CategoryEntity(
                id = 1L,
                name = "Groceries",
                transactionType = TransactionType.EXPENSE,
                iconKey = "groceries",
                isDefault = true,
                isActive = true
            )
        )

        val originalTransactions = listOf(
            TransactionEntity(
                id = 101L,
                type = TransactionType.EXPENSE,
                amount = 250000L, // Rs. 2,500.00
                categoryId = 1L,
                description = "Weekly market shopping",
                timestamp = 1761234567890L,
                paymentMethod = PaymentMethod.CASH
            )
        )

        val originalBudgets = listOf(
            BudgetEntity(
                id = 1L,
                categoryId = null,
                month = 10,
                year = 2026,
                amount = 10000000L, // Rs. 100,000.00
                warningThresholdPercent = 80
            )
        )

        val originalPlans = listOf(
            PlannedExpenseEntity(
                id = 1L,
                description = "Electricity Bill",
                expectedAmount = 1500000L, // Rs. 15,000.00
                categoryId = 1L,
                plannedDate = 1761999999999L,
                status = PlannedExpenseStatus.PLANNED
            )
        )

        val payload = BackupPayload(
            backupVersion = 1,
            appVersion = "1.2.0",
            createdAtMillis = 1761000000000L,
            startingBalancePaisa = 5000000L,
            protectedReservePaisa = 2000000L,
            categories = originalCategories,
            transactions = originalTransactions,
            budgets = originalBudgets,
            plannedExpenses = originalPlans
        )

        val jsonString = payload.toJsonString()
        val restored = BackupPayload.fromJsonString(jsonString)

        assertEquals(payload.backupVersion, restored.backupVersion)
        assertEquals(payload.appVersion, restored.appVersion)
        assertEquals(payload.startingBalancePaisa, restored.startingBalancePaisa)
        assertEquals(payload.protectedReservePaisa, restored.protectedReservePaisa)
        assertEquals(1, restored.categories.size)
        assertEquals("Groceries", restored.categories[0].name)
        assertEquals(1, restored.transactions.size)
        assertEquals(250000L, restored.transactions[0].amount)
        assertEquals(1, restored.budgets.size)
        assertEquals(10000000L, restored.budgets[0].amount)
        assertEquals(1, restored.plannedExpenses.size)
        assertEquals(1500000L, restored.plannedExpenses[0].expectedAmount)
    }
}
