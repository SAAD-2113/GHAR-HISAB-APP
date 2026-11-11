package com.sindantechnologies.gharhisab.util.reminder

import android.app.*
import android.content.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sindantechnologies.gharhisab.MainActivity
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity

object NotificationHelper {
    const val CHANNEL_ID = "gharhisab_planned_expenses"
    fun createNotificationChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminders_title), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
    fun showExpenseReminderNotification(context: Context, expense: PlannedExpenseEntity, reminderType: String): Boolean {
        createNotificationChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // No amounts/titles or direct write actions on a potentially locked device.
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.reminders_title))
            .setContentText(context.getString(R.string.reminder_private_message))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open).setAutoCancel(true).build()
        return try {
            NotificationManagerCompat.from(context).notify(expense.id.toString(), 1, notification)
            true
        } catch (_: SecurityException) { false }
    }
}
