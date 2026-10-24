package com.sindantechnologies.gharhisab.util.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.sindantechnologies.gharhisab.GharHisabApplication
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles reminder alarms and direct notification actions for planned expenses.
 */
class ExpenseReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EXPENSE_ALARM = "com.sindantechnologies.gharhisab.ACTION_EXPENSE_REMINDER"
        const val ACTION_MARK_PAID = "com.sindantechnologies.gharhisab.ACTION_MARK_EXPENSE_PAID"
        const val EXTRA_EXPENSE_ID = "extra_expense_id"
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? GharHisabApplication ?: return
        val repository = app.container.plannedExpenseRepository
        val expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, -1L)
        if (expenseId == -1L) return

        val pendingResult = goAsync()

        when (intent.action) {
            ACTION_EXPENSE_ALARM -> {
                val reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: "DUE_DATE"
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val expense = repository.getPlannedExpenseById(expenseId)
                        if (expense != null && expense.status == PlannedExpenseStatus.PLANNED) {
                            NotificationHelper.showExpenseReminderNotification(context, expense, reminderType)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_MARK_PAID -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.markAsPaid(expenseId)
                        NotificationManagerCompat.from(context).cancel(expenseId.toInt())
                        ReminderScheduler.cancelReminders(context, expenseId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            else -> {
                pendingResult.finish()
            }
        }
    }
}
