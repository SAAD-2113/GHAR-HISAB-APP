package com.sindantechnologies.gharhisab.ui.screens.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.ui.theme.ExpenseRed
import com.sindantechnologies.gharhisab.ui.theme.IncomeGreen
import com.sindantechnologies.gharhisab.util.Constants
import com.sindantechnologies.gharhisab.util.IconMapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChanged(it) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
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
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = Dimens.ElevationLg,
                shadowElevation = Dimens.ElevationXl,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd)
                ) {
                    Button(
                        onClick = { viewModel.saveTransaction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ButtonHeight),
                        enabled = !uiState.isSaving,
                        shape = RoundedCornerShape(Dimens.RadiusMd)
                    ) {
                        Text(
                            text = if (uiState.isSaving) "Saving..." else "Save Transaction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Type Segmented Button with clear Income vs Expense badges
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm)
            ) {
                SegmentedButton(
                    selected = uiState.transactionType == TransactionType.EXPENSE,
                    onClick = { viewModel.onTransactionTypeChanged(TransactionType.EXPENSE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (uiState.transactionType == TransactionType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    Text("Expense", fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = uiState.transactionType == TransactionType.INCOME,
                    onClick = { viewModel.onTransactionTypeChanged(TransactionType.INCOME) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = if (uiState.transactionType == TransactionType.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    Text("Income", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Amount Input
            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg),
                label = { Text("Amount") },
                prefix = { Text("${Constants.CURRENCY_SYMBOL} ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { { Text(it) } },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = if (uiState.transactionType == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Description Input
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg),
                label = { Text("Description (Optional)") },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Date Picker Trigger
            OutlinedTextField(
                value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(uiState.timestamp)),
                onValueChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg)
                    .clickable { showDatePicker = true },
                label = { Text("Date") },
                readOnly = true,
                enabled = false,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = {
                    Icon(Icons.Filled.CalendarToday, contentDescription = "Select Date")
                }
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Payment Method - Responsive FlowRow Chips
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Dimens.SpacingLg)
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
            ) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = uiState.selectedPaymentMethod == method,
                        onClick = { viewModel.onPaymentMethodSelected(method) },
                        label = { Text(method.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (uiState.categoryError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = Dimens.SpacingLg)
            )
            if (uiState.categoryError != null) {
                Text(
                    text = uiState.categoryError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Dimens.SpacingLg)
                )
            }
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            // Chunked rows of 4 for natural responsive scrolling
            uiState.categories.chunked(4).forEach { rowCategories ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowCategories.forEach { category ->
                        CategoryItem(
                            category = category,
                            isSelected = uiState.selectedCategory?.id == category.id,
                            onClick = { viewModel.onCategorySelected(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - rowCategories.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingHuge))
        }
    }
}

@Composable
private fun CategoryItem(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(Dimens.SpacingXs)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            shape = RoundedCornerShape(Dimens.RadiusMd),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) Dimens.ElevationMd else Dimens.ElevationNone),
            modifier = Modifier.size(52.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = IconMapper.getIcon(category.iconKey),
                    contentDescription = category.name,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(Dimens.SpacingXs))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
