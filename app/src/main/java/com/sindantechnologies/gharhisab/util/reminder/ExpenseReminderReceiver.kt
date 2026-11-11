package com.sindantechnologies.gharhisab.util.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sindantechnologies.gharhisab.GharHisabApplication
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import kotlinx.coroutines.*

class ExpenseReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_EXPENSE_ALARM = "com.sindantechnologies.gharhisab.ACTION_EXPENSE_REMINDER"
        const val EXTRA_EXPENSE_ID = "extra_expense_id"
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_EXPENSE_ALARM) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as GharHisabApplication
                val plan = app.container.plannedExpenseRepository.getPlannedExpenseById(intent.getLongExtra(EXTRA_EXPENSE_ID, -1)) ?: return@launch
                val offset = intent.getIntExtra("offset", 0)
                if (plan.status != PlannedExpenseStatus.PLANNED || !plan.remindersEnabled ||
                    plan.updatedAt != intent.getLongExtra("revision", -1) || ReminderScheduler.wasDelivered(context, plan, offset)) return@launch
                if (NotificationHelper.showExpenseReminderNotification(context, plan, offset.toString())) {
                    ReminderScheduler.markDelivered(context, plan, offset)
                }
            } finally { result.finish() }
        }
    }
}
