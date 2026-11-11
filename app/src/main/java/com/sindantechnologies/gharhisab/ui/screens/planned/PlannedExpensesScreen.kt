package com.sindantechnologies.gharhisab.ui.screens.planned

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.room.entity.*
import com.sindantechnologies.gharhisab.domain.model.*
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlannedExpensesScreen(viewModel: PlannedExpensesViewModel, onNavigateBack: () -> Unit) {
    val schedulingFailed by com.sindantechnologies.gharhisab.util.reminder.ReminderScheduler.schedulingFailed.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<PlannedExpenseEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<PlannedExpenseEntity?>(null) }
    var deleting by remember { mutableStateOf<PlannedExpenseEntity?>(null) }
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }
    LaunchedEffect(state.operationSerial) { creating = false; editing = null; paying = null; deleting = null }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.plans_title)) }, navigationIcon = {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
        }) },
        floatingActionButton = { FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, stringResource(R.string.plan_add)) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!permissionGranted) item {
                Card { Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.reminder_permission_explanation))
                    TextButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }) { Text(stringResource(R.string.reminders_enable)) }
                } }
            }
            item {
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now()
                val unpaid = state.plannedExpenses.filter { it.status == PlannedExpenseStatus.PLANNED }
                val due = unpaid.count { Instant.ofEpochMilli(it.plannedDate).atZone(zone).toLocalDate() == today }
                val overdue = unpaid.count { Instant.ofEpochMilli(it.plannedDate).atZone(zone).toLocalDate() < today }
                val end = today.withDayOfMonth(today.lengthOfMonth()).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                val total = runCatching { Recurrence.commitments(unpaid, end) }.getOrDefault(Long.MAX_VALUE)
                Card { Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.plans_summary, due, overdue))
                    Text(stringResource(R.string.commitments_month, CurrencyFormatter.formatPaisa(total)))
                    Text(stringResource(R.string.reminder_delivery_note), style = MaterialTheme.typography.bodySmall)
                } }
            }
            if (schedulingFailed) item { Text(stringResource(R.string.reminder_schedule_failed), color = MaterialTheme.colorScheme.error) }
            if (state.operationFailed) item { Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error) }
            items(state.plannedExpenses, key = { it.id }) { plan ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text(plan.description, style = MaterialTheme.typography.titleMedium)
                    Text(CurrencyFormatter.formatPaisa(plan.expectedAmount))
                    Text(Instant.ofEpochMilli(plan.plannedDate).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)))
                    val status = when {
                        plan.status == PlannedExpenseStatus.PAID -> R.string.plan_paid
                        plan.status == PlannedExpenseStatus.CANCELLED -> R.string.plan_cancelled
                        Instant.ofEpochMilli(plan.plannedDate).atZone(ZoneId.systemDefault()).toLocalDate() < LocalDate.now() -> R.string.plan_overdue
                        else -> R.string.plan_upcoming
                    }
                    Text(stringResource(status))
                    if (plan.recurrence != null && plan.recurrence != "NONE") Text(stringResource(R.string.plan_recurring))
                    plan.notes?.let { Text(it) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (plan.status == PlannedExpenseStatus.PLANNED) {
                            TextButton(onClick = { editing = plan }, enabled = !state.isSaving) { Text(stringResource(R.string.plan_edit_postpone)) }
                            TextButton(onClick = { viewModel.cancelPlannedExpense(plan.id) }, enabled = !state.isSaving) { Text(stringResource(R.string.action_cancel)) }
                            Button(onClick = { paying = plan }, enabled = !state.isSaving) { Text(stringResource(R.string.plan_mark_paid)) }
                        }
                        TextButton(onClick = { deleting = plan }, enabled = !state.isSaving) { Text(stringResource(R.string.action_delete)) }
                    }
                } }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    if (creating || editing != null) PlanEditor(editing, state.expenseCategories, state.isSaving, state.operationFailed,
        onDismiss = { creating = false; editing = null }, onSave = viewModel::savePlan)
    paying?.let { plan ->
        var paidAmount by remember { mutableStateOf(CurrencyFormatter.formatPaisaRaw(plan.expectedAmount)) }
        var method by remember { mutableStateOf(PaymentMethod.CASH) }
        AlertDialog(onDismissRequest = { paying = null }, title = { Text(stringResource(R.string.plan_mark_paid)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.plan_paid_confirm, plan.description, CurrencyFormatter.formatPaisa(plan.expectedAmount)))
                OutlinedTextField(paidAmount, { paidAmount = it }, label = { Text(stringResource(R.string.transaction_amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                FlowRow { PaymentMethod.entries.forEach { option ->
                    FilterChip(selected = method == option, onClick = { method = option }, label = { Text(stringResource(com.sindantechnologies.gharhisab.ui.screens.transaction.paymentMethodLabel(option))) })
                } }
                if (state.operationFailed) Text(stringResource(R.string.operation_failed))
            }
        }, confirmButton = { Button(onClick = { viewModel.markAsPaid(plan.id, method, CurrencyFormatter.parseRupeesToPaisa(paidAmount)) }, enabled = !state.isSaving && (CurrencyFormatter.parseRupeesToPaisa(paidAmount) ?: 0) > 0) { Text(stringResource(R.string.action_ok)) } },
            dismissButton = { TextButton(onClick = { paying = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    deleting?.let { plan -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(stringResource(R.string.action_delete)) },
        text = { Text(stringResource(R.string.plan_delete_confirm)) },
        confirmButton = { Button(onClick = { viewModel.deletePlannedExpense(plan) }, enabled = !state.isSaving) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } }) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PlanEditor(original: PlannedExpenseEntity?, categories: List<CategoryEntity>, saving: Boolean, failed: Boolean, onDismiss: () -> Unit, onSave: (PlannedExpenseEntity) -> Unit) {
    var title by remember { mutableStateOf(original?.description.orEmpty()) }
    var amount by remember { mutableStateOf(original?.let { CurrencyFormatter.formatPaisaRaw(it.expectedAmount) }.orEmpty()) }
    var category by remember { mutableStateOf(original?.categoryId ?: categories.firstOrNull()?.id) }
    var date by remember { mutableStateOf(original?.plannedDate ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(original?.notes.orEmpty()) }
    var recurrence by remember { mutableStateOf(original?.recurrence ?: "NONE") }
    var interval by remember { mutableStateOf((original?.recurrenceInterval ?: 1).toString()) }
    var enabled by remember { mutableStateOf(original?.remindersEnabled ?: true) }
    var offsets by remember { mutableStateOf(original?.reminderDays?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(0, 1, 3)) }
    var picker by remember { mutableStateOf(false) }
    if (picker) {
        val localDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
        val value = rememberDatePickerState(initialSelectedDateMillis = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { picker = false }, confirmButton = {
            TextButton(onClick = {
                value.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
                picker = false
            }) { Text(stringResource(R.string.action_ok)) }
        }) { DatePicker(value) }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.plan_add)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.plan_title)) })
            OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.transaction_amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            FlowRow { categories.forEach { cat -> FilterChip(category == cat.id, { category = cat.id }, label = { Text(cat.name) }) } }
            TextButton(onClick = { picker = true }) { Text(stringResource(R.string.transaction_date) + ": " + Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()) }
            OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.transaction_description)) })
            FlowRow { listOf("NONE" to R.string.repeat_none, "WEEKLY" to R.string.repeat_weekly, "MONTHLY" to R.string.repeat_monthly, "YEARLY" to R.string.repeat_yearly).forEach { (key, label) ->
                FilterChip(recurrence == key, { recurrence = key }, label = { Text(stringResource(label)) })
            } }
            if (recurrence != "NONE") OutlinedTextField(interval, { interval = it }, label = { Text(stringResource(R.string.repeat_interval)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Row { Checkbox(enabled, { enabled = it }); Text(stringResource(R.string.reminders_title)) }
            if (enabled) FlowRow { listOf(0, 1, 3, 7).forEach { offset ->
                FilterChip(offset in offsets, { offsets = if (offset in offsets) offsets - offset else offsets + offset }, label = { Text(stringResource(R.string.reminder_days_before, offset)) })
            } }
            if (failed) Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        val money = CurrencyFormatter.parseRupeesToPaisa(amount)
        Button(enabled = !saving && title.isNotBlank() && money != null && money > 0 && category != null && interval.toIntOrNull() in 1..120 && (!enabled || offsets.isNotEmpty()), onClick = {
            val item = original ?: PlannedExpenseEntity(description = title, expectedAmount = money!!, categoryId = category!!, plannedDate = date)
            onSave(item.copy(description = title.trim(), expectedAmount = money!!, categoryId = category!!, plannedDate = date, notes = notes.ifBlank { null },
                recurrence = recurrence, recurrenceInterval = interval.toInt(), recurrenceAnchorDay = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).dayOfMonth,
                remindersEnabled = enabled, reminderDays = offsets.sorted().joinToString(",").ifEmpty { "0" }))
        }) { Text(stringResource(R.string.transaction_save)) }
    }, dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.action_cancel)) } })
}
