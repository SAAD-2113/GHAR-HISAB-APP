package com.sindantechnologies.gharhisab.ui.screens.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.ui.theme.ExpenseRed
import com.sindantechnologies.gharhisab.ui.theme.ExpenseRedLight
import com.sindantechnologies.gharhisab.ui.theme.IncomeGreen
import com.sindantechnologies.gharhisab.ui.theme.IncomeGreenLight
import com.sindantechnologies.gharhisab.ui.theme.MidnightBackground
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import com.sindantechnologies.gharhisab.util.IconMapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme = MaterialTheme.colorScheme.background == MidnightBackground

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
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
                .padding(paddingValues),
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(Dimens.RadiusMd),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXs)
            )

            // Filter Chips Row: Type & Date
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Dimens.SpacingLg),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
            ) {
                // Type filters
                item {
                    FilterChip(
                        selected = uiState.selectedType == null,
                        onClick = { viewModel.onTypeFilterSelected(null) },
                        label = { Text("All Types") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedType == TransactionType.EXPENSE,
                        onClick = {
                            viewModel.onTypeFilterSelected(
                                if (uiState.selectedType == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                            )
                        },
                        label = { Text("Expense") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedType == TransactionType.INCOME,
                        onClick = {
                            viewModel.onTypeFilterSelected(
                                if (uiState.selectedType == TransactionType.INCOME) null else TransactionType.INCOME
                            )
                        },
                        label = { Text("Income") }
                    )
                }

                // Date filters
                DateRangeFilter.entries.forEach { range ->
                    item {
                        FilterChip(
                            selected = uiState.selectedDateRange == range,
                            onClick = { viewModel.onDateRangeFilterSelected(range) },
                            label = { Text(range.displayName) }
                        )
                    }
                }
            }

            // Summary Totals Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Income: ${CurrencyFormatter.formatPaisa(uiState.filteredIncomeTotalPaisa)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDarkTheme) IncomeGreenLight else IncomeGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Expense: ${CurrencyFormatter.formatPaisa(uiState.filteredExpenseTotalPaisa)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDarkTheme) ExpenseRedLight else ExpenseRed,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.SpacingXxl),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(Dimens.IconXxl),
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingLg))
                        Text(
                            text = "No matching transactions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "Try a different search term" else "Record an income or expense to see it here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = Dimens.SpacingXs)
                ) {
                    items(uiState.transactions, key = { it.id }) { transaction ->
                        val category = uiState.categories[transaction.categoryId]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXs)
                                .clickable { onNavigateToEditTransaction(transaction.id) },
                            shape = RoundedCornerShape(Dimens.RadiusMd),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.SpacingMd),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Category Icon + Type indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (category != null) IconMapper.getIcon(category.iconKey) else Icons.Filled.Receipt,
                                        contentDescription = category?.name,
                                        tint = if (transaction.type == TransactionType.INCOME) {
                                            if (isDarkTheme) IncomeGreenLight else IncomeGreen
                                        } else {
                                            if (isDarkTheme) ExpenseRedLight else ExpenseRed
                                        },
                                        modifier = Modifier.size(Dimens.IconLg)
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.SpacingMd))

                                    Column {
                                        Text(
                                            text = transaction.description ?: category?.name ?: stringResource(R.string.transaction_no_description),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.timestamp)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (transaction.paymentMethod != null) {
                                                Text(
                                                    text = " • ${transaction.paymentMethod.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(Dimens.SpacingSm))

                                // Clear Amount with visual distinction
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (transaction.type == TransactionType.INCOME) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = if (transaction.type == TransactionType.INCOME) {
                                                if (isDarkTheme) IncomeGreenLight else IncomeGreen
                                            } else {
                                                if (isDarkTheme) ExpenseRedLight else ExpenseRed
                                            },
                                            modifier = Modifier.size(Dimens.IconSm)
                                        )
                                        Spacer(modifier = Modifier.width(Dimens.SpacingXxs))
                                        Text(
                                            text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"} ${CurrencyFormatter.formatPaisa(transaction.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (transaction.type == TransactionType.INCOME) {
                                                if (isDarkTheme) IncomeGreenLight else IncomeGreen
                                            } else {
                                                if (isDarkTheme) ExpenseRedLight else ExpenseRed
                                            },
                                        )
                                    }
                                    Text(
                                        text = if (transaction.type == TransactionType.INCOME) "Income" else "Expense",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
