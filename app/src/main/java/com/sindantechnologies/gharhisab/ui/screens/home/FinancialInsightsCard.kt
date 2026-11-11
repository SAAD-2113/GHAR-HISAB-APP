package com.sindantechnologies.gharhisab.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.domain.model.SpendingForecast
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import com.sindantechnologies.gharhisab.util.reminder.InsightNotifications

@Composable
fun FinancialInsightsCard(forecast: SpendingForecast) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("insight_notifications", 0) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    LaunchedEffect(forecast, enabled) { if (enabled) InsightNotifications.check(context, forecast) }
    Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (forecast.preliminary) R.string.insights_preliminary else R.string.insights_estimate))
            Text(stringResource(R.string.insights_daily, CurrencyFormatter.formatPaisa(forecast.dailyLimit)))
            Text(stringResource(R.string.insights_average, CurrencyFormatter.formatPaisa(forecast.averageDaily), forecast.observedDays))
            Text(stringResource(R.string.commitments_month, CurrencyFormatter.formatPaisa(forecast.commitments)))
            if (forecast.shortfall > 0) Text(stringResource(R.string.insights_shortfall, CurrencyFormatter.formatPaisa(forecast.shortfall)), color = MaterialTheme.colorScheme.error)
            if (forecast.depletionDate != null) Text(stringResource(R.string.insights_depletion, forecast.depletionDate.toString()))
            else Text(stringResource(R.string.insights_insufficient))
            if (forecast.increasing) Text(stringResource(R.string.insights_increasing))
            if (forecast.averageDaily > forecast.dailyLimit) Text(stringResource(R.string.insights_above_limit))
            Text(stringResource(R.string.insights_formula), style = MaterialTheme.typography.bodySmall)
            Row { Switch(enabled, { enabled = it; prefs.edit().putBoolean("enabled", it).apply() }); Text(stringResource(R.string.insights_notifications)) }
            if (enabled && !androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) Text(stringResource(R.string.reminder_permission_explanation))
        }
    }
}
