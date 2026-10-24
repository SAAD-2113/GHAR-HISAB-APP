package com.sindantechnologies.gharhisab.ui.screens.planned

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.ui.theme.ExpenseRed
import com.sindantechnologies.gharhisab.ui.theme.IncomeGreen
import com.sindantechnologies.gharhisab.util.Constants
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import com.sindantechnologies.gharhisab.util.IconMapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedExpensesScreen(
    viewModel: PlannedExpensesViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilterIndex by remember { mutableIntStateOf(1) } // 0: All, 1: Upcoming, 2: Paid, 3: Cancelled
    var showAddDialog by remember { mutableStateOf(false) }

    // Reschedule dialog state
    var itemToReschedule by remember { mutableStateOf<PlannedExpenseEntity?>(null) }
    var itemToMarkPaid by remember { mutableStateOf<PlannedExpenseEntity?>(null) }

    val filterStatus = when (selectedFilterIndex) {
        1 -> PlannedExpenseStatus.PLANNED
        2 -> PlannedExpenseStatus.PAID
        3 -> PlannedExpenseStatus.CANCELLED
        else -> null
    }

    val filteredList = uiState.plannedExpenses.filter { item ->
        filterStatus == null || item.status == filterStatus
    }.sortedBy { it.plannedDate }

    // Reschedule DatePicker
    if (itemToReschedule != null) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = itemToReschedule!!.plannedDate)
        DatePickerDialog(
            onDismissRequest = { itemToReschedule = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { newDate ->
                        viewModel.reschedulePlannedExpense(itemToReschedule!!.id, newDate)
                    }
                    itemToReschedule = null
                }) {
                    Text("Reschedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToReschedule = null }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    // Mark as Paid Confirmation Dialog
    if (itemToMarkPaid != null) {
        var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
        AlertDialog(
            onDismissRequest = { itemToMarkPaid = null },
            title = { Text("Confirm Payment") },
            text = {
                Column {
                    Text(
                        text = "Mark \"${itemToMarkPaid!!.description}\" as paid? This will create an actual recorded expense transaction in your database.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                    Text(
                        text = "Amount: ${CurrencyFormatter.formatPaisa(itemToMarkPaid!!.expectedAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    Text(text = "Payment Method:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)) {
                        PaymentMethod.entries.take(4).forEach { method ->
                            FilterChip(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method },
                                label = { Text(method.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAsPaid(itemToMarkPaid!!.id, selectedPaymentMethod)
                        itemToMarkPaid = null
                    }
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToMarkPaid = null }) { Text("Cancel") }
            }
        )
    }

    // Add Planned Expense Dialog
    if (showAddDialog) {
        AddPlannedExpenseDialog(
            categories = uiState.expenseCategories,
            onDismiss = { showAddDialog = false },
            onSave = { desc, amount, catId, date, notes, recurrence ->
                viewModel.addPlannedExpense(desc, amount, catId, date, notes, recurrence)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planned Spending") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Planned Expense")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Summary Cards Header
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpacingLg),
                    shape = RoundedCornerShape(Dimens.RadiusLg),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMd)
                ) {
                    Column(modifier = Modifier.padding(Dimens.SpacingLg)) {
                        Text(
                            text = "Upcoming Commitments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "This Week",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatPaisa(uiState.expectedExpensesThisWeekPaisa),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Before Month-End",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatPaisa(uiState.expectedExpensesThisMonthPaisa),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Est. Remaining",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatPaisa(uiState.expectedRemainingFundsPaisa),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.expectedRemainingFundsPaisa >= 0L) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // Filter Tabs
            item {
                val tabs = listOf("All", "Upcoming", "Paid", "Cancelled")
                ScrollableTabRow(
                    selectedTabIndex = selectedFilterIndex,
                    edgePadding = Dimens.SpacingLg,
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedFilterIndex == index,
                            onClick = { selectedFilterIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingXxl),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(Dimens.IconXxl)
                            )
                            Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                            Text(
                                text = "No planned expenses found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Schedule upcoming expenses to forecast your spending.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    val category = uiState.categories[item.categoryId]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXs),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(Dimens.SpacingMd)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (category != null) {
                                        Icon(
                                            imageVector = IconMapper.getIcon(category.iconKey),
                                            contentDescription = category.name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(Dimens.IconMd)
                                        )
                                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                                    }
                                    Column {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Due: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.plannedDate))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.formatPaisa(item.expectedAmount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Status Badge
                                    when (item.status) {
                                        PlannedExpenseStatus.PLANNED -> {
                                            Text(
                                                text = "Planned",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        PlannedExpenseStatus.PAID -> {
                                            Text(
                                                text = "✓ Paid",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = IncomeGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        PlannedExpenseStatus.CANCELLED -> {
                                            Text(
                                                text = "Cancelled",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Actions for planned items
                            if (item.status == PlannedExpenseStatus.PLANNED) {
                                Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { itemToReschedule = item }) {
                                        Text("Reschedule")
                                    }
                                    TextButton(onClick = { viewModel.cancelPlannedExpense(item.id) }) {
                                        Text("Cancel")
                                    }
                                    Button(onClick = { itemToMarkPaid = item }) {
                                        Text("Mark as Paid")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimens.SpacingHuge))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlannedExpenseDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (description: String, amountPaisa: Long, categoryId: Long, plannedDate: Long, notes: String?, recurrence: String) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf("NONE") }

    var showDatePicker by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = dateState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Future Expense") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Expected Amount") },
                    prefix = { Text("${Constants.CURRENCY_SYMBOL} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                // Date Picker trigger
                OutlinedTextField(
                    value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Planned Date") },
                    trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = CurrencyFormatter.parseRupeesToPaisa(amountText)
                    val cat = selectedCategory
                    if (description.isNotBlank() && amount != null && amount > 0L && cat != null) {
                        onSave(description, amount, cat.id, selectedDateMillis, notes.ifBlank { null }, recurrence)
                    }
                },
                enabled = description.isNotBlank() && amountText.isNotBlank() && selectedCategory != null
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
