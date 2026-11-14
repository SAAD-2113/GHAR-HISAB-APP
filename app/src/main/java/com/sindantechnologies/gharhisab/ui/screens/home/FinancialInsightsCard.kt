package com.sindantechnologies.gharhisab.ui.screens.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sindantechnologies.gharhisab.ui.theme.AppTextButton as TextButton
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.domain.model.SpendingForecast
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import com.sindantechnologies.gharhisab.util.reminder.InsightNotifications
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun FinancialInsightsCard(forecast: SpendingForecast) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("insight_notifications", 0) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var permissionGranted by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var explainPermission by rememberSaveable { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionGranted = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permissionGranted = granted }
    LaunchedEffect(forecast, enabled, permissionGranted) { if (enabled && permissionGranted) InsightNotifications.check(context, forecast) }
    FinancialInsightsContent(forecast, enabled, onAlertsChanged = {
        enabled = it
        prefs.edit().putBoolean("enabled", it).apply()
        if (it && !permissionGranted) explainPermission = true
    }, permissionGranted = permissionGranted, onRequestPermission = { explainPermission = true })
    if (explainPermission) AlertDialog(
        onDismissRequest = { explainPermission = false },
        title = { Text(stringResource(R.string.insights_notifications)) },
        text = { Text(stringResource(R.string.insights_permission_help)) },
        confirmButton = { TextButton(onClick = {
            explainPermission = false
            if (Build.VERSION.SDK_INT >= 33 && !prefs.getBoolean("permission_requested", false)) {
                prefs.edit().putBoolean("permission_requested", true).apply()
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text(stringResource(R.string.reminders_enable)) } },
        dismissButton = { TextButton(onClick = { explainPermission = false }) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Stateless layout makes the display and accessible controls testable without posting notifications. */
@Composable
internal fun FinancialInsightsContent(
    forecast: SpendingForecast,
    alertsEnabled: Boolean,
    onAlertsChanged: (Boolean) -> Unit,
    permissionGranted: Boolean = true,
    onRequestPermission: () -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("financial_insights"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = colors.secondaryContainer, contentColor = colors.onSecondaryContainer) {
                    Icon(Icons.Default.Insights, null, Modifier.padding(10.dp).size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(if (forecast.preliminary) R.string.insights_early_label else R.string.insights_estimate_label),
                        style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = colors.secondaryContainer, contentColor = colors.onSecondaryContainer) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.insights_daily_label), style = MaterialTheme.typography.labelLarge)
                    Text(CurrencyFormatter.formatPaisa(forecast.dailyLimit), modifier = Modifier.testTag("insights_daily_amount"),
                        style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.insights_daily_caption), style = MaterialTheme.typography.bodySmall)
                }
            }
            val fontScale = LocalDensity.current.fontScale
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 280.dp && fontScale <= 1.25f) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InsightMetric(stringResource(R.string.insights_rate_label), forecast.averageDaily, Modifier.weight(1f))
                        InsightMetric(stringResource(R.string.insights_commitments_label), forecast.commitments, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InsightMetric(stringResource(R.string.insights_rate_label), forecast.averageDaily)
                        InsightMetric(stringResource(R.string.insights_commitments_label), forecast.commitments)
                    }
                }
            }
            val needsAttention = forecast.shortfall > 0 || (forecast.balanceKnown && forecast.averageDaily > forecast.dailyLimit)
            val outlook = when {
                forecast.shortfall > 0 -> stringResource(R.string.insights_shortfall, CurrencyFormatter.formatPaisa(forecast.shortfall))
                !forecast.balanceKnown -> stringResource(R.string.insights_unknown_balance)
                forecast.observedDays < 3 -> stringResource(R.string.insights_need_history)
                forecast.depletionDate != null -> stringResource(R.string.insights_depletion, forecast.depletionDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                forecast.averageDaily > forecast.dailyLimit -> stringResource(R.string.insights_above_limit)
                else -> stringResource(R.string.insights_no_depletion)
            }
            Surface(shape = RoundedCornerShape(14.dp), color = if (needsAttention) colors.errorContainer else colors.surfaceVariant,
                contentColor = if (needsAttention) colors.onErrorContainer else colors.onSurfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(if (needsAttention) Icons.Default.Info else Icons.Default.CalendarToday, null, Modifier.size(20.dp))
                    Text(outlook, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            // The whole row is one accessible switch target. The visual switch has fixed space,
            // so large text and RTL cannot squeeze it out of view.
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant, border = BorderStroke(1.dp, colors.outline)) {
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("insights_alert_toggle")
                        .toggleable(value = alertsEnabled, role = Role.Switch, onValueChange = onAlertsChanged)
                        .semantics(mergeDescendants = true) {}.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.insights_notifications), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.insights_notifications_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Switch(checked = alertsEnabled, onCheckedChange = null, modifier = Modifier.testTag("insights_alert_control"),
                        thumbContent = if (alertsEnabled) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary,
                            checkedBorderColor = colors.primary, checkedIconColor = colors.primary,
                            uncheckedThumbColor = colors.onSurfaceVariant, uncheckedTrackColor = colors.surface,
                            uncheckedBorderColor = colors.onSurfaceVariant))
                }
            }
            if (alertsEnabled && !permissionGranted) {
                Column {
                    Text(stringResource(R.string.insights_permission_blocked), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    TextButton(onClick = onRequestPermission) { Text(stringResource(R.string.reminders_enable)) }
                }
            }
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().testTag("insights_calculation_toggle")) {
                Text(stringResource(if (expanded) R.string.insights_details_hide else R.string.insights_details_show), Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            if (expanded) Column(Modifier.testTag("insights_calculation_details"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.insights_estimate), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(stringResource(R.string.insights_average, CurrencyFormatter.formatPaisa(forecast.averageDaily), forecast.observedDays), style = MaterialTheme.typography.bodySmall)
                if (forecast.increasing) Text(stringResource(R.string.insights_increasing), style = MaterialTheme.typography.bodySmall)
                if (forecast.averageDaily > forecast.dailyLimit) Text(stringResource(R.string.insights_above_limit), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.insights_formula), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InsightMetric(label: String, amount: Long, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(CurrencyFormatter.formatPaisa(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
