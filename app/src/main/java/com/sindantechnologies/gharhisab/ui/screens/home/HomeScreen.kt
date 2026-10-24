package com.sindantechnologies.gharhisab.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.ui.components.SummaryCard
import com.sindantechnologies.gharhisab.ui.screens.transaction.VoiceEntryDialog
import com.sindantechnologies.gharhisab.ui.theme.BalanceBlue
import com.sindantechnologies.gharhisab.ui.theme.BalanceBlueLight
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.ui.theme.ExpenseRed
import com.sindantechnologies.gharhisab.ui.theme.ExpenseRedLight
import com.sindantechnologies.gharhisab.ui.theme.IncomeGreen
import com.sindantechnologies.gharhisab.ui.theme.IncomeGreenLight
import com.sindantechnologies.gharhisab.ui.theme.MidnightBackground
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import com.sindantechnologies.gharhisab.util.IconMapper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern, responsive Home Dashboard screen for GharHisab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToPlannedExpenses: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme = MaterialTheme.colorScheme.background == MidnightBackground
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showVoiceDialog by remember { mutableStateOf(false) }

    if (showVoiceDialog) {
        VoiceEntryDialog(
            categories = uiState.allCategoriesList,
            onDismiss = { showVoiceDialog = false },
            onSaveTransaction = { transaction ->
                viewModel.saveVoiceTransaction(transaction)
                scope.launch {
                    snackbarHostState.showSnackbar("Transaction recorded successfully!")
                }
            },
            onFallbackManualEntry = {
                onNavigateToAddTransaction()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.monthName.isNotBlank()) {
                            Text(
                                text = uiState.monthName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.home_add_transaction),
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Quick Actions Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.SpacingXs),
                contentPadding = PaddingValues(horizontal = Dimens.SpacingLg),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                item {
                    Button(
                        onClick = { showVoiceDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(Dimens.RadiusFull),
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text("Voice Entry", style = MaterialTheme.typography.labelMedium)
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onNavigateToBudgets,
                        shape = RoundedCornerShape(Dimens.RadiusFull),
                    ) {
                        Icon(Icons.Filled.PieChart, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text("Budgets", style = MaterialTheme.typography.labelMedium)
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onNavigateToPlannedExpenses,
                        shape = RoundedCornerShape(Dimens.RadiusFull),
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                        Text("Planned", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            // Summary Cards Row (Income, Expense, Net Monthly Cash Flow)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
            ) {
                SummaryCard(
                    title = stringResource(R.string.home_income),
                    amount = CurrencyFormatter.formatPaisa(uiState.totalIncome),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconTint = if (isDarkTheme) IncomeGreenLight else IncomeGreen,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    title = stringResource(R.string.home_expense),
                    amount = CurrencyFormatter.formatPaisa(uiState.totalExpense),
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    iconTint = if (isDarkTheme) ExpenseRedLight else ExpenseRed,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    title = "Net Flow",
                    amount = CurrencyFormatter.formatPaisa(uiState.netMonthlyCashFlow),
                    icon = Icons.Filled.AccountBalanceWallet,
                    iconTint = if (isDarkTheme) BalanceBlueLight else BalanceBlue,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Daily Safe-to-Spend Recommendation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.IconMd)
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text(
                                text = "Daily Spending Limit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${uiState.dailyRecommendation.remainingDays} days left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                    Text(
                        text = CurrencyFormatter.formatPaisaCompact(uiState.dailyRecommendation.dailyLimitPaisa),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.dailyRecommendation.isDeficit) ExpenseRed else MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Suggested safe-to-spend per day based on available funds and upcoming commitments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                    // Contextual breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spendable: ${CurrencyFormatter.formatPaisa(uiState.dailyRecommendation.spendableFundsPaisa)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Upcoming: ${CurrencyFormatter.formatPaisa(uiState.dailyRecommendation.upcomingPlannedCommitmentsPaisa)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.dailyRecommendation.warningMessage != null) {
                        Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                        Text(
                            text = uiState.dailyRecommendation.warningMessage!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.dailyRecommendation.isDeficit) ExpenseRed else Color(0xFFFFA000),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Budget Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg)
                    .clickable(onClick = onNavigateToBudgets),
                shape = RoundedCornerShape(Dimens.RadiusLg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSm)
            ) {
                Column(modifier = Modifier.padding(Dimens.SpacingLg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.IconMd)
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(onClick = onNavigateToBudgets) {
                            Text("Manage")
                        }
                    }

                    if (uiState.overallBudget == null) {
                        Text(
                            text = "No budget set for this month. Tap to configure your spending targets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val progressColor = when {
                            uiState.isBudgetExceeded -> ExpenseRed
                            uiState.isBudgetWarning -> Color(0xFFFFA000)
                            else -> IncomeGreen
                        }
                        LinearProgressIndicator(
                            progress = { uiState.budgetProgressPercent.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spent: ${CurrencyFormatter.formatPaisa(uiState.totalExpense)} / ${CurrencyFormatter.formatPaisa(uiState.overallBudget!!.amount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Remaining: ${CurrencyFormatter.formatPaisa(uiState.remainingBudget)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.remainingBudget >= 0L) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Upcoming Planned Commitments Section
            if (uiState.upcomingPlannedExpenses.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Commitments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onNavigateToPlannedExpenses) {
                        Text("View All")
                    }
                }
                uiState.upcomingPlannedExpenses.forEach { plan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXxs)
                            .clickable(onClick = onNavigateToPlannedExpenses),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.SpacingMd),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plan.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Due: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(plan.plannedDate))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatPaisa(plan.expectedAmount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.SpacingMd))
            }

            // Recent Transactions Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (uiState.recentTransactions.isNotEmpty()) {
                    TextButton(onClick = onNavigateToTransactions) {
                        Text(text = stringResource(R.string.home_view_all))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXs))

            if (uiState.recentTransactions.isEmpty()) {
                // Friendly First-Use Empty State Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg),
                    shape = RoundedCornerShape(Dimens.RadiusLg),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSm),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingXxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(Dimens.IconXxl),
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingLg))
                        Text(
                            text = stringResource(R.string.home_no_transactions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                        Text(
                            text = stringResource(R.string.home_no_transactions_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingLg))
                        Button(
                            onClick = onNavigateToAddTransaction,
                            shape = RoundedCornerShape(Dimens.RadiusFull)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
                            Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                            Text("Record Your First Transaction")
                        }
                    }
                }
            } else {
                // Recent transactions list
                uiState.recentTransactions.forEach { transaction ->
                    val category = uiState.categories[transaction.categoryId]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingXxs)
                            .clickable { onNavigateToEditTransaction(transaction.id) },
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.SpacingMd),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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
                                    modifier = Modifier.size(Dimens.IconMd)
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
                                    Text(
                                        text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))

                            Text(
                                text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"} ${CurrencyFormatter.formatPaisa(transaction.amount)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.type == TransactionType.INCOME) {
                                    if (isDarkTheme) IncomeGreenLight else IncomeGreen
                                } else {
                                    if (isDarkTheme) ExpenseRedLight else ExpenseRed
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingHuge))
        }
    }
}
