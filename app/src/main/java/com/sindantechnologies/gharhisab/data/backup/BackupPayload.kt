package com.sindantechnologies.gharhisab.data.backup

import com.sindantechnologies.gharhisab.data.local.room.entity.BudgetEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encapsulates the entire GharHisab financial state for encrypted backup and restore.
 */
data class BackupPayload(
    val backupVersion: Int = 1,
    val appVersion: String,
    val createdAtMillis: Long,
    val startingBalancePaisa: Long,
    val protectedReservePaisa: Long,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val plannedExpenses: List<PlannedExpenseEntity>
) {

    fun toJsonString(): String {
        val root = JSONObject().apply {
            put("backupVersion", backupVersion)
            put("appVersion", appVersion)
            put("createdAtMillis", createdAtMillis)
            put("startingBalancePaisa", startingBalancePaisa)
            put("protectedReservePaisa", protectedReservePaisa)

            // Categories
            val catArray = JSONArray()
            categories.forEach { cat ->
                catArray.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("transactionType", cat.transactionType.name)
                    put("iconKey", cat.iconKey)
                    put("isDefault", cat.isDefault)
                    put("isActive", cat.isActive)
                })
            }
            put("categories", catArray)

            // Transactions
            val txArray = JSONArray()
            transactions.forEach { tx ->
                txArray.put(JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type.name)
                    put("amount", tx.amount)
                    put("categoryId", tx.categoryId)
                    put("description", tx.description ?: JSONObject.NULL)
                    put("timestamp", tx.timestamp)
                    put("paymentMethod", tx.paymentMethod?.name ?: JSONObject.NULL)
                    put("createdAt", tx.createdAt)
                    put("updatedAt", tx.updatedAt)
                })
            }
            put("transactions", txArray)

            // Budgets
            val bgArray = JSONArray()
            budgets.forEach { bg ->
                bgArray.put(JSONObject().apply {
                    put("id", bg.id)
                    put("categoryId", bg.categoryId ?: JSONObject.NULL)
                    put("month", bg.month)
                    put("year", bg.year)
                    put("amount", bg.amount)
                    put("warningThresholdPercent", bg.warningThresholdPercent)
                })
            }
            put("budgets", bgArray)

            // Planned Expenses
            val peArray = JSONArray()
            plannedExpenses.forEach { pe ->
                peArray.put(JSONObject().apply {
                    put("id", pe.id)
                    put("description", pe.description)
                    put("expectedAmount", pe.expectedAmount)
                    put("categoryId", pe.categoryId)
                    put("plannedDate", pe.plannedDate)
                    put("notes", pe.notes ?: JSONObject.NULL)
                    put("recurrence", pe.recurrence ?: JSONObject.NULL)
                    put("status", pe.status.name)
                    put("actualTransactionId", pe.actualTransactionId ?: JSONObject.NULL)
                    put("createdAt", pe.createdAt)
                    put("updatedAt", pe.updatedAt)
                })
            }
            put("plannedExpenses", peArray)
        }
        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): BackupPayload {
            val root = JSONObject(jsonStr)

            val backupVersion = root.optInt("backupVersion", 1)
            val appVersion = root.optString("appVersion", "1.2.0")
            val createdAtMillis = root.optLong("createdAtMillis", System.currentTimeMillis())
            val startingBalancePaisa = root.optLong("startingBalancePaisa", 0L)
            val protectedReservePaisa = root.optLong("protectedReservePaisa", 0L)

            // Categories
            val catList = mutableListOf<CategoryEntity>()
            val catArray = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                catList.add(
                    CategoryEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        transactionType = TransactionType.valueOf(obj.getString("transactionType")),
                        iconKey = obj.getString("iconKey"),
                        isDefault = obj.optBoolean("isDefault", false),
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }

            // Transactions
            val txList = mutableListOf<TransactionEntity>()
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                txList.add(
                    TransactionEntity(
                        id = obj.getLong("id"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        amount = obj.getLong("amount"),
                        categoryId = obj.getLong("categoryId"),
                        description = if (obj.isNull("description")) null else obj.getString("description"),
                        timestamp = obj.getLong("timestamp"),
                        paymentMethod = if (obj.isNull("paymentMethod")) null else PaymentMethod.valueOf(obj.getString("paymentMethod")),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // Budgets
            val bgList = mutableListOf<BudgetEntity>()
            val bgArray = root.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until bgArray.length()) {
                val obj = bgArray.getJSONObject(i)
                bgList.add(
                    BudgetEntity(
                        id = obj.getLong("id"),
                        categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                        month = obj.getInt("month"),
                        year = obj.getInt("year"),
                        amount = obj.getLong("amount"),
                        warningThresholdPercent = obj.optInt("warningThresholdPercent", 80)
                    )
                )
            }

            // Planned Expenses
            val peList = mutableListOf<PlannedExpenseEntity>()
            val peArray = root.optJSONArray("plannedExpenses") ?: JSONArray()
            for (i in 0 until peArray.length()) {
                val obj = peArray.getJSONObject(i)
                peList.add(
                    PlannedExpenseEntity(
                        id = obj.getLong("id"),
                        description = obj.getString("description"),
                        expectedAmount = obj.getLong("expectedAmount"),
                        categoryId = obj.getLong("categoryId"),
                        plannedDate = obj.getLong("plannedDate"),
                        notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                        recurrence = if (obj.isNull("recurrence")) null else obj.getString("recurrence"),
                        status = PlannedExpenseStatus.valueOf(obj.optString("status", PlannedExpenseStatus.PLANNED.name)),
                        actualTransactionId = if (obj.isNull("actualTransactionId")) null else obj.getLong("actualTransactionId"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            return BackupPayload(
                backupVersion = backupVersion,
                appVersion = appVersion,
                createdAtMillis = createdAtMillis,
                startingBalancePaisa = startingBalancePaisa,
                protectedReservePaisa = protectedReservePaisa,
                categories = catList,
                transactions = txList,
                budgets = bgList,
                plannedExpenses = peList
            )
        }
    }
}
