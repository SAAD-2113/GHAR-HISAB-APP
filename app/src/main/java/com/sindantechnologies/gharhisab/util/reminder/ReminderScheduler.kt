package com.sindantechnologies.gharhisab.util.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import java.util.Calendar

/**
 * Schedules and cancels reminder alarms for planned expenses.
 */
object ReminderScheduler {

    private const val REQUEST_CODE_DUE_DATE = 0
    private const val REQUEST_CODE_1_DAY = 1
    private const val REQUEST_CODE_3_DAYS = 2

    /**
     * Schedules reminders for a planned expense.
     * Fires at 9:00 AM on:
     * - The due date
     * - 1 day before
     * - 3 days before
     */
    fun scheduleReminders(context: Context, expense: PlannedExpenseEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        // 1. Due date at 9:00 AM
        val dueDateTarget = getTargetTime(expense.plannedDate, daysOffset = 0, hour = 9)
        if (dueDateTarget > now) {
            scheduleSingleAlarm(context, alarmManager, expense.id, dueDateTarget, REQUEST_CODE_DUE_DATE, "DUE_DATE")
        }

        // 2. 1 day before at 9:00 AM
        val oneDayBeforeTarget = getTargetTime(expense.plannedDate, daysOffset = -1, hour = 9)
        if (oneDayBeforeTarget > now) {
            scheduleSingleAlarm(context, alarmManager, expense.id, oneDayBeforeTarget, REQUEST_CODE_1_DAY, "1_DAY")
        }

        // 3. 3 days before at 9:00 AM
        val threeDaysBeforeTarget = getTargetTime(expense.plannedDate, daysOffset = -3, hour = 9)
        if (threeDaysBeforeTarget > now) {
            scheduleSingleAlarm(context, alarmManager, expense.id, threeDaysBeforeTarget, REQUEST_CODE_3_DAYS, "3_DAYS")
        }
    }

    private fun scheduleSingleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        expenseId: Long,
        triggerAtMillis: Long,
        offsetCode: Int,
        reminderType: String
    ) {
        val intent = Intent(context, ExpenseReminderReceiver::class.java).apply {
            action = ExpenseReminderReceiver.ACTION_EXPENSE_ALARM
            putExtra(ExpenseReminderReceiver.EXTRA_EXPENSE_ID, expenseId)
            putExtra(ExpenseReminderReceiver.EXTRA_REMINDER_TYPE, reminderType)
        }

        val requestCode = (expenseId * 10 + offsetCode).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Fallback for safety
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Cancels all scheduled reminder alarms for an expense.
     */
    fun cancelReminders(context: Context, expenseId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        listOf(REQUEST_CODE_DUE_DATE, REQUEST_CODE_1_DAY, REQUEST_CODE_3_DAYS).forEach { offsetCode ->
            val intent = Intent(context, ExpenseReminderReceiver::class.java).apply {
                action = ExpenseReminderReceiver.ACTION_EXPENSE_ALARM
            }
            val requestCode = (expenseId * 10 + offsetCode).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun getTargetTime(baseDateMillis: Long, daysOffset: Int, hour: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseDateMillis
            add(Calendar.DAY_OF_YEAR, daysOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
