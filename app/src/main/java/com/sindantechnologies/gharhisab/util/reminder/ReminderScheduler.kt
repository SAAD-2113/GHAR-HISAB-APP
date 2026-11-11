package com.sindantechnologies.gharhisab.util.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import java.time.Instant
import java.time.ZoneId

/** Inexact, persistent alarms. Android can defer them; no exact-alarm permission is needed. */
object ReminderScheduler {
    private val schedulingFailedState = kotlinx.coroutines.flow.MutableStateFlow(false)
    val schedulingFailed: kotlinx.coroutines.flow.StateFlow<Boolean> = schedulingFailedState
    private val offsets = listOf(0, 1, 3, 7)
    private fun preferences(context: Context) = context.getSharedPreferences("reminder_registry", Context.MODE_PRIVATE)
    private fun intent(context: Context, id: Long, offset: Int) = Intent(context, ExpenseReminderReceiver::class.java).apply {
        action = ExpenseReminderReceiver.ACTION_EXPENSE_ALARM
        data = Uri.parse("gharhisab://reminder/$id/$offset")
        putExtra(ExpenseReminderReceiver.EXTRA_EXPENSE_ID, id)
        putExtra("offset", offset)
    }

    fun reconcile(context: Context, plans: List<PlannedExpenseEntity>) {
        var failed = false
        try {
            val prefs = preferences(context)
            val oldIds = prefs.getStringSet("ids", emptySet()).orEmpty().toSet()
            oldIds.forEach { value ->
                try { value.toLongOrNull()?.let { cancelReminders(context, it) } }
                catch (_: Exception) { failed = true }
            }
            prefs.edit().putStringSet("ids", plans.map { it.id.toString() }.toSet()).apply()
            plans.filter { it.status == PlannedExpenseStatus.PLANNED }.forEach {
                try { scheduleReminders(context, it) }
                catch (_: Exception) { failed = true }
            }
        } catch (_: Exception) { failed = true }
        // Saving a plan already succeeded. Report scheduling separately so retry cannot duplicate it.
        schedulingFailedState.value = failed
    }

    fun deliveryKey(plan: PlannedExpenseEntity, offset: Int) = "${plan.id}:${plan.plannedDate}:$offset"
    fun wasDelivered(context: Context, plan: PlannedExpenseEntity, offset: Int) =
        preferences(context).getBoolean(deliveryKey(plan, offset), false)
    fun markDelivered(context: Context, plan: PlannedExpenseEntity, offset: Int) {
        preferences(context).edit().putBoolean(deliveryKey(plan, offset), true).apply()
    }

    fun scheduleReminders(context: Context, expense: PlannedExpenseEntity) {
        cancelReminders(context, expense.id)
        if (!expense.remindersEnabled || expense.status != PlannedExpenseStatus.PLANNED) return
        val manager = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val due = Instant.ofEpochMilli(expense.plannedDate).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        expense.reminderDays.split(',').mapNotNull { it.toIntOrNull() }.distinct().filter { it in offsets }.forEach { offset ->
            if (wasDelivered(context, expense, offset)) return@forEach
            val day = due.minusDays(offset.toLong())
            if (day < today && offset != 0) return@forEach
            val target = maxOf(now + 10_000L, day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli())
            val alarm = intent(context, expense.id, offset).putExtra("revision", expense.updatedAt)
            val pending = PendingIntent.getBroadcast(context, 0, alarm, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pending)
        }
    }

    fun cancelReminders(context: Context, expenseId: Long) {
        val manager = context.getSystemService(AlarmManager::class.java)
        offsets.forEach { offset ->
            PendingIntent.getBroadcast(context, 0, intent(context, expenseId, offset), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                manager.cancel(it)
                it.cancel()
            }
            // Cancel request-code alarms from v2 as well.
            val legacy = Intent(context, ExpenseReminderReceiver::class.java).setAction(ExpenseReminderReceiver.ACTION_EXPENSE_ALARM)
            PendingIntent.getBroadcast(context, (expenseId * 10 + offsets.indexOf(offset)).toInt(), legacy, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                manager.cancel(it); it.cancel()
            }
        }
        NotificationManagerCompat.from(context).cancel(expenseId.toString(), 1)
        NotificationManagerCompat.from(context).cancel(expenseId.toInt())
    }
}
