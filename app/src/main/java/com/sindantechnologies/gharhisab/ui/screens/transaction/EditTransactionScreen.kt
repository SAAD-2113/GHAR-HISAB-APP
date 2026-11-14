package com.sindantechnologies.gharhisab.ui.screens.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.sindantechnologies.gharhisab.ui.theme.appOutlinedTextFieldColors
import com.sindantechnologies.gharhisab.ui.theme.appDatePickerColors
import com.sindantechnologies.gharhisab.ui.theme.AppFilterChip as FilterChip
import com.sindantechnologies.gharhisab.ui.theme.AppTextButton as TextButton
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.ui.theme.isMidnight
import com.sindantechnologies.gharhisab.ui.theme.appSegmentedButtonColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.ui.theme.ExpenseRed
import com.sindantechnologies.gharhisab.ui.theme.IncomeGreen
import com.sindantechnologies.gharhisab.util.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionScreen(
    viewModel: EditTransactionViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    LaunchedEffect(uiState.saveFailed) {
        if (uiState.saveFailed) scrollState.animateScrollTo(0)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        if (uiState.isSaved || uiState.isDeleted) {
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
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState, colors = appDatePickerColors())
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.transaction_delete)) },
            text = { Text(stringResource(R.string.transaction_delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteTransaction()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (MaterialTheme.colorScheme.isMidnight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transaction_edit)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!uiState.isLoading) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        bottomBar = {
            if (!uiState.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = Dimens.ElevationLg,
                    shadowElevation = Dimens.ElevationXl,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd)
                    ) {
                        Button(
                            onClick = { viewModel.saveTransaction() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.ButtonHeight),
                            enabled = !uiState.isSaving,
                            shape = RoundedCornerShape(Dimens.RadiusMd)
                        ) {
                            Text(
                                text = stringResource(if (uiState.isSaving) R.string.transaction_saving else R.string.transaction_update),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                if (uiState.saveFailed) {
                    Text(
                        stringResource(R.string.transaction_save_failed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Dimens.SpacingLg),
                    )
                }
                // Type Segmented Button
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm)
                ) {
                    SegmentedButton(
                        colors = appSegmentedButtonColors(),
                        selected = uiState.transactionType == TransactionType.EXPENSE,
                        onClick = { viewModel.onTransactionTypeChanged(TransactionType.EXPENSE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (uiState.transactionType == TransactionType.EXPENSE) {
                                if (MaterialTheme.colorScheme.isMidnight) MaterialTheme.colorScheme.onPrimary else ExpenseRed
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    ) {
                        Text(stringResource(R.string.home_expense), fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        colors = appSegmentedButtonColors(),
                        selected = uiState.transactionType == TransactionType.INCOME,
                        onClick = { viewModel.onTransactionTypeChanged(TransactionType.INCOME) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = if (uiState.transactionType == TransactionType.INCOME) {
                                if (MaterialTheme.colorScheme.isMidnight) MaterialTheme.colorScheme.onPrimary else IncomeGreen
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    ) {
                        Text(stringResource(R.string.home_income), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                // Amount
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = viewModel::onAmountChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg),
                    label = { Text(stringResource(R.string.transaction_amount)) },
                    prefix = { Text("${Constants.CURRENCY_SYMBOL} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { { Text(it) } },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = if (uiState.transactionType == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = appOutlinedTextFieldColors(),
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                // Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg),
                    label = { Text(stringResource(R.string.transaction_description)) },
                    singleLine = true,
                    colors = appOutlinedTextFieldColors(),
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                // Date
                OutlinedTextField(
                    value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(uiState.timestamp)),
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg)
                        .clickable { showDatePicker = true },
                    label = { Text(stringResource(R.string.transaction_date)) },
                    readOnly = true,
                    enabled = false,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = {
                        Icon(Icons.Filled.CalendarToday, contentDescription = stringResource(R.string.transaction_select_date))
                    }
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                // Payment Method - FlowRow Chips
                Text(
                    text = stringResource(R.string.transaction_payment_method),
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
                            label = { Text(stringResource(paymentMethodLabel(method))) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                // Category
                Text(
                    text = stringResource(R.string.transaction_category),
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

                TransactionCategories(
                    categories = uiState.categories,
                    selectedId = uiState.selectedCategory?.id,
                    onSelect = viewModel::onCategorySelected,
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingHuge))
            }
        }
    }
}
