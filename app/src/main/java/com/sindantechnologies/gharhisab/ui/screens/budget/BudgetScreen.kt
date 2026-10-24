package com.sindantechnologies.gharhisab.ui.screens.budget

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.ui.theme.ExpenseRed
import com.sindantechnologies.gharhisab.ui.theme.IncomeGreen
import com.sindantechnologies.gharhisab.util.Constants
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import com.sindantechnologies.gharhisab.util.IconMapper
import java.text.DateFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showOverallBudgetDialog by remember { mutableStateOf(false) }
    var showCategoryBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForBudget by remember { mutableStateOf<CategoryEntity?>(null) }

    val monthName = DateFormatSymbols(Locale.getDefault()).months.getOrNull(uiState.month - 1) ?: "Month"

    if (showOverallBudgetDialog) {
        var amountText by remember {
            mutableStateOf(uiState.overallBudget?.amount?.let { (it / 100L).toString() } ?: "")
        }
        var warningThresholdText by remember {
            mutableStateOf(uiState.overallBudget?.warningThresholdPercent?.toString() ?: "80")
        }

        AlertDialog(
            onDismissRequest = { showOverallBudgetDialog = false },
            title = { Text("Set Monthly Budget") },
            text = {
                Column {
                    Text(
                        text = "Set overall spending limit for $monthName ${uiState.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monthly Budget Amount") },
                        prefix = { Text("${Constants.CURRENCY_SYMBOL} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    OutlinedTextField(
                        value = warningThresholdText,
                        onValueChange = { warningThresholdText = it },
                        label = { Text("Warning Threshold (%)") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paisa = CurrencyFormatter.parseRupeesToPaisa(amountText)
                        val threshold = warningThresholdText.toIntOrNull() ?: 80
                        if (paisa != null && paisa > 0L) {
                            viewModel.setOverallBudget(paisa, threshold)
                            showOverallBudgetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverallBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCategoryBudgetDialog) {
        var expanded by remember { mutableStateOf(false) }
        var chosenCategory by remember { mutableStateOf(selectedCategoryForBudget ?: uiState.expenseCategories.firstOrNull()) }
        var amountText by remember { mutableStateOf("") }
        var warningThresholdText by remember { mutableStateOf("80") }

        AlertDialog(
            onDismissRequest = { showCategoryBudgetDialog = false },
            title = { Text("Set Category Budget") },
            text = {
                Column {
                    Text(
                        text = "Set category limit for $monthName ${uiState.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                    // Category dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = chosenCategory?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            uiState.expenseCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        chosenCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Category Budget Amount") },
                        prefix = { Text("${Constants.CURRENCY_SYMBOL} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    OutlinedTextField(
                        value = warningThresholdText,
                        onValueChange = { warningThresholdText = it },
                        label = { Text("Warning Threshold (%)") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cat = chosenCategory
                        val paisa = CurrencyFormatter.parseRupeesToPaisa(amountText)
                        val threshold = warningThresholdText.toIntOrNull() ?: 80
                        if (cat != null && paisa != null && paisa > 0L) {
                            viewModel.setCategoryBudget(cat.id, paisa, threshold)
                            showCategoryBudgetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets — $monthName ${uiState.year}") },
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.SpacingLg),
        ) {
            // Overall Budget Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.RadiusLg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMd)
            ) {
                Column(modifier = Modifier.padding(Dimens.SpacingLg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Overall Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Spending limit for $monthName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showOverallBudgetDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Overall Budget")
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                    if (uiState.overallBudget == null) {
                        Text(
                            text = "No overall budget set for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                        Button(
                            onClick = { showOverallBudgetDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set Monthly Budget")
                        }
                    } else {
                        val budgetAmount = uiState.overallBudget!!.amount
                        val spent = uiState.overallSpentPaisa
                        val remaining = uiState.overallRemainingPaisa
                        val progress = uiState.overallProgressPercent

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatPaisa(spent),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isOverallExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Budget",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatPaisa(budgetAmount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Remaining Budget",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatPaisa(remaining),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining >= 0L) IncomeGreen else ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                        val progressColor = when {
                            uiState.isOverallExceeded -> ExpenseRed
                            uiState.isOverallWarning -> Color(0xFFFFA000)
                            else -> IncomeGreen
                        }

                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}% spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = progressColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uiState.isOverallExceeded) {
                                Text(
                                    text = "⚠ Budget Exceeded!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (uiState.isOverallWarning) {
                                Text(
                                    text = "Warning: Approaching Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFA000),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            // Category Budgets Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(
                    onClick = {
                        selectedCategoryForBudget = null
                        showCategoryBudgetDialog = true
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                    Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                    Text("Add Category")
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            if (uiState.categoryBudgets.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingLg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(Dimens.IconLg)
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                        Text(
                            text = "No category budgets set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Create category budgets to track spending by department.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                uiState.categoryBudgets.forEach { catBudget ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.SpacingXs),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(Dimens.SpacingMd)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = IconMapper.getIcon(catBudget.category.iconKey),
                                        contentDescription = catBudget.category.name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(Dimens.IconMd)
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                                    Text(
                                        text = catBudget.category.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteBudget(catBudget.budget) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(Dimens.IconSm)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Dimens.SpacingXs))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Spent: ${CurrencyFormatter.formatPaisa(catBudget.spentPaisa)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (catBudget.isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Budget: ${CurrencyFormatter.formatPaisa(catBudget.budget.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                            val catColor = when {
                                catBudget.isExceeded -> ExpenseRed
                                catBudget.isWarning -> Color(0xFFFFA000)
                                else -> IncomeGreen
                            }

                            LinearProgressIndicator(
                                progress = { catBudget.progressPercent.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = catColor,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            if (catBudget.isExceeded || catBudget.isWarning) {
                                Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                                Text(
                                    text = if (catBudget.isExceeded) "⚠ Exceeded by ${CurrencyFormatter.formatPaisa(-catBudget.remainingPaisa)}" else "Approaching budget limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingHuge))
        }
    }
}
