package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.data.backup.BackupPayload
import com.sindantechnologies.gharhisab.data.local.room.entity.*
import com.sindantechnologies.gharhisab.domain.model.*
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

class BackupValidationTest {
    private fun payload() = BackupPayload(appVersion = "3.0.0", createdAtMillis = 1,
        startingBalancePaisa = 100, protectedReservePaisa = 0,
        categories = listOf(CategoryEntity(1, "Income", TransactionType.INCOME, "home")),
        transactions = listOf(TransactionEntity(1, TransactionType.INCOME, 1, 1, description = null, timestamp = 1, paymentMethod = null)),
        budgets = emptyList(), plannedExpenses = emptyList())
    @Test fun overflowInStartingBalancePlusIncomeIsRejected() {
        assertTrue(runCatching { payload().copy(startingBalancePaisa = Long.MAX_VALUE).validate() }.isFailure)
        payload().copy(startingBalancePaisa = Long.MAX_VALUE - 1).validate()
    }
    @Test fun fractionalStringAndNullMonetaryFieldsAreRejected() {
        for (value in listOf(1.5, "100", JSONObject.NULL)) {
            val root = JSONObject(payload().toJsonString()).put("startingBalancePaisa", value)
            assertTrue(runCatching { BackupPayload.fromJsonString(root.toString()) }.isFailure)
            val txRoot = JSONObject(payload().toJsonString())
            txRoot.getJSONArray("transactions").getJSONObject(0).put("amount", value)
            assertTrue(runCatching { BackupPayload.fromJsonString(txRoot.toString()) }.isFailure)
        }
    }
    @Test fun legacyPayloadWithoutNewOptionalFieldsStillRestores() {
        val root = JSONObject(payload().toJsonString()).put("backupVersion", 1)
        root.remove("language"); root.remove("theme")
        root.getJSONArray("transactions").getJSONObject(0).remove("plannedExpenseId")
        val value = BackupPayload.fromJsonString(root.toString())
        assertEquals(100L, value.startingBalancePaisa)
        assertEquals(0L, value.protectedReservePaisa)
        assertEquals(1L, value.transactions.single().amount)
    }
}
