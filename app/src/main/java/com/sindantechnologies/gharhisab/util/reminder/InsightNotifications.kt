package com.sindantechnologies.gharhisab.util.reminder

import android.content.Context
import android.app.*
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sindantechnologies.gharhisab.MainActivity
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.domain.model.SpendingForecast
import java.time.LocalDate

object InsightNotifications {
    fun check(context: Context, forecast: SpendingForecast) {
        if (!forecast.balanceKnown || forecast.preliminary) return
        val prefs = context.getSharedPreferences("insight_notifications", 0)
        if (!prefs.getBoolean("enabled", false)) return
        val day = LocalDate.now().toEpochDay()
        val depletion = forecast.depletionDate?.toEpochDay() ?: Long.MAX_VALUE
        val risk = when { forecast.shortfall > 0 -> "shortfall"; depletion <= day + 7 -> "depletion"; forecast.averageDaily > forecast.dailyLimit -> "rate"; else -> return }
        val lastDay = prefs.getLong("day", 0)
        val lastDepletion = prefs.getLong("depletion", Long.MAX_VALUE)
        if (day == lastDay || (risk == prefs.getString("risk", "") && depletion >= lastDepletion - 3 && day - lastDay < 7)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("insights", context.getString(R.string.insights_title), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        try {
            NotificationManagerCompat.from(context).notify("insights", 1, NotificationCompat.Builder(context, "insights")
                .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle(context.getString(R.string.insights_title))
                .setContentText(context.getString(R.string.insights_notification_message)).setContentIntent(open)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).build())
            prefs.edit().putLong("day", day).putLong("depletion", depletion).putString("risk", risk).apply()
        } catch (_: SecurityException) { /* Permission can be revoked between checking and posting. */ }
    }
}
