package com.sindantechnologies.gharhisab.util.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sindantechnologies.gharhisab.GharHisabApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules all active planned expense reminders after device reboot or package update.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_TIME_CHANGED && intent.action != Intent.ACTION_TIMEZONE_CHANGED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val app = context.applicationContext as? GharHisabApplication ?: return
        val repository = app.container.plannedExpenseRepository
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeExpenses = repository.getActivePlannedExpensesList()
                ReminderScheduler.reconcile(context, activeExpenses)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
