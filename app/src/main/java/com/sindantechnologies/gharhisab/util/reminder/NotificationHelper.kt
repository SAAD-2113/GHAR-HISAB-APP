package com.sindantechnologies.gharhisab.util.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sindantechnologies.gharhisab.MainActivity
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper for managing notification channels and posting high-priority
 * reminders for planned expenses and financial dues.
 */
object NotificationHelper {

    const val CHANNEL_ID = "gharhisab_planned_expenses"
    private const val CHANNEL_NAME = "Planned Expenses & Dues"
    private const val CHANNEL_DESC = "Notifications and timely reminders for upcoming bills and planned expenses"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showExpenseReminderNotification(
        context: Context,
        expense: PlannedExpenseEntity,
        reminderType: String // "3_DAYS", "1_DAY", "DUE_DATE"
    ) {
        createNotificationChannel(context)

        val formattedAmount = CurrencyFormatter.formatPaisa(expense.expectedAmount)
        val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(expense.plannedDate))

        val title = when (reminderType) {
            "DUE_DATE" -> "Due Today: ${expense.description}"
            "1_DAY" -> "Due Tomorrow: ${expense.description}"
            else -> "Upcoming Bill: ${expense.description}"
        }

        val text = when (reminderType) {
            "DUE_DATE" -> "Payment of $formattedAmount is due today. Tap to mark as paid."
            "1_DAY" -> "Payment of $formattedAmount is scheduled for tomorrow ($formattedDate)."
            else -> "Reminder: $formattedAmount is scheduled for $formattedDate."
        }

        // Tap to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            expense.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark as Paid direct action
        val paidIntent = Intent(context, ExpenseReminderReceiver::class.java).apply {
            action = ExpenseReminderReceiver.ACTION_MARK_PAID
            putExtra(ExpenseReminderReceiver.EXTRA_EXPENSE_ID, expense.id)
        }
        val paidPendingIntent = PendingIntent.getBroadcast(
            context,
            (expense.id * 100 + 1).toInt(),
            paidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Mark Paid",
                paidPendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(expense.id.toInt(), builder.build())
        } catch (_: SecurityException) {
            // Handled when POST_NOTIFICATIONS is not yet granted
        }
    }
}
