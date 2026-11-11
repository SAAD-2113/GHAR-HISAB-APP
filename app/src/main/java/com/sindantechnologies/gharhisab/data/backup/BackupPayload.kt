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
    val backupVersion: Int = 2,
    val appVersion: String,
    val createdAtMillis: Long,
    val startingBalancePaisa: Long,
    val protectedReservePaisa: Long,
    val language: String = "en",
    val theme: String = "system",
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val plannedExpenses: List<PlannedExpenseEntity>
) {

    fun toJsonString(): String {
        val root = JSONObject().apply {
            put("backupVersion", backupVersion)
            put("language", language)
            put("theme", theme)
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
                    put("plannedExpenseId", tx.plannedExpenseId ?: JSONObject.NULL)
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
                    put("recurrenceInterval", pe.recurrenceInterval)
                    put("recurrenceAnchorDay", pe.recurrenceAnchorDay)
                    put("remindersEnabled", pe.remindersEnabled)
                    put("reminderDays", pe.reminderDays)
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

    fun validate() {
        fun ids(values: List<Long>) { require(values.all { it > 0 } && values.size == values.toSet().size) }
        ids(categories.map { it.id }); ids(transactions.map { it.id }); ids(budgets.map { it.id }); ids(plannedExpenses.map { it.id })
        require(backupVersion in 1..2 && createdAtMillis > 0)
        require(listOf(categories.size, transactions.size, budgets.size, plannedExpenses.size).all { it <= 100000 })
        require(startingBalancePaisa >= 0 && protectedReservePaisa >= 0)
        val cats = categories.associateBy { it.id }
        require(transactions.all { it.amount > 0 && it.timestamp > 0 && cats[it.categoryId]?.transactionType == it.type })
        require(budgets.all { it.amount > 0 && it.month in 1..12 && it.year in 1900..9999 && (it.categoryId == null || it.categoryId in cats) })
        require(plannedExpenses.all { it.description.isNotBlank() && it.expectedAmount > 0 && it.plannedDate > 0 &&
            cats[it.categoryId]?.transactionType == TransactionType.EXPENSE && it.recurrenceInterval in 1..120 &&
            it.recurrence in listOf(null, "NONE", "WEEKLY", "MONTHLY", "YEARLY") && it.recurrenceAnchorDay in 0..31 &&
            it.reminderDays.split(',').all { offset -> offset.toIntOrNull() in listOf(0,1,3,7) } })
        // Reject totals that cannot be represented by the app's integer monetary model.
        transactions.fold(0L) { total, row -> Math.addExact(total, row.amount) }
        // Check the same intermediates used by the dashboard, including future-dated income.
        val income = transactions.filter { it.type == TransactionType.INCOME }.fold(0L) { total, row -> Math.addExact(total, row.amount) }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.fold(0L) { total, row -> Math.addExact(total, row.amount) }
        val balance = Math.subtractExact(Math.addExact(startingBalancePaisa, income), expenses)
        Math.subtractExact(balance, protectedReservePaisa)
        plannedExpenses.fold(0L) { total, row -> Math.addExact(total, row.expectedAmount) }
    }

    companion object {
        private fun JSONObject.exactLong(key: String): Long {
            // Both supported versions always wrote integer monetary fields. Reject missing,
            // null, string or fractional values instead of silently coercing them to zero.
            val value = get(key)
            require(value is Int || value is Long) { "Invalid integer field" }
            return (value as Number).toLong()
        }
        fun fromJsonString(jsonStr: String): BackupPayload {
            val root = JSONObject(jsonStr)

            val backupVersion = root.getInt("backupVersion")
            require(backupVersion in 1..2) { "Unsupported backup version" }
            listOf("categories", "transactions", "budgets", "plannedExpenses").forEach { require(root.getJSONArray(it).length() <= 100000) }
            require(root.getLong("createdAtMillis") > 0)
            val appVersion = root.optString("appVersion", "1.2.0")
            val createdAtMillis = root.optLong("createdAtMillis", System.currentTimeMillis())
            val startingBalancePaisa = root.exactLong("startingBalancePaisa")
            val protectedReservePaisa = root.exactLong("protectedReservePaisa")

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
                        amount = obj.exactLong("amount"),
                        categoryId = obj.getLong("categoryId"),
                        description = if (obj.isNull("description")) null else obj.getString("description"),
                        timestamp = obj.getLong("timestamp"),
                        plannedExpenseId = if (obj.isNull("plannedExpenseId")) null else obj.getLong("plannedExpenseId"),
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
                        amount = obj.exactLong("amount"),
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
                        expectedAmount = obj.exactLong("expectedAmount"),
                        categoryId = obj.getLong("categoryId"),
                        plannedDate = obj.getLong("plannedDate"),
                        notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                        recurrence = if (obj.isNull("recurrence")) null else obj.getString("recurrence"),
                        recurrenceInterval = obj.optInt("recurrenceInterval", 1),
                        recurrenceAnchorDay = obj.optInt("recurrenceAnchorDay", 0),
                        remindersEnabled = obj.optBoolean("remindersEnabled", true),
                        reminderDays = obj.optString("reminderDays", "0,1,3"),
                        status = PlannedExpenseStatus.valueOf(obj.optString("status", PlannedExpenseStatus.PLANNED.name)),
                        actualTransactionId = if (obj.isNull("actualTransactionId")) null else obj.getLong("actualTransactionId"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            return BackupPayload(
                backupVersion = backupVersion,
                language = root.optString("language", "en"),
                theme = root.optString("theme", "system"),
                appVersion = appVersion,
                createdAtMillis = createdAtMillis,
                startingBalancePaisa = startingBalancePaisa,
                protectedReservePaisa = protectedReservePaisa,
                categories = catList,
                transactions = txList,
                budgets = bgList,
                plannedExpenses = peList
            ).also { it.validate() }
        }
    }
}
