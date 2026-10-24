package com.sindantechnologies.gharhisab.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.Language
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.ui.components.SettingsGroup
import com.sindantechnologies.gharhisab.ui.components.SettingsItem
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.util.Constants
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import kotlinx.coroutines.launch

/**
 * Functional Settings screen with real persistent user configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferencesRepository: UserPreferencesRepository,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentLanguage by userPreferencesRepository.languageFlow.collectAsStateWithLifecycle(initialValue = Language.ENGLISH)
    val currentTheme by userPreferencesRepository.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.FOLLOW_SYSTEM)
    val startingBalance by userPreferencesRepository.startingBalanceFlow.collectAsStateWithLifecycle(initialValue = 0L)
    val protectedReserve by userPreferencesRepository.protectedReserveFlow.collectAsStateWithLifecycle(initialValue = 0L)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showStartingBalanceDialog by remember { mutableStateOf(false) }
    var showProtectedReserveDialog by remember { mutableStateOf(false) }

    // Language Dialog
    if (showLanguageDialog) {
        var tempLang by remember { mutableStateOf(currentLanguage) }
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_selection_title)) },
            text = {
                Column {
                    Language.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = tempLang == lang,
                                    onClick = { tempLang = lang }
                                )
                                .padding(vertical = Dimens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tempLang == lang,
                                onClick = { tempLang = lang }
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text("${lang.displayName} (${lang.nativeName})")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        userPreferencesRepository.setLanguage(tempLang)
                        showLanguageDialog = false
                        snackbarHostState.showSnackbar("Language updated to ${tempLang.displayName}")
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        var tempTheme by remember { mutableStateOf(currentTheme) }
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme_selection_title)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = tempTheme == theme,
                                    onClick = { tempTheme = theme }
                                )
                                .padding(vertical = Dimens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tempTheme == theme,
                                onClick = { tempTheme = theme }
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text(theme.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        userPreferencesRepository.setThemeMode(tempTheme)
                        showThemeDialog = false
                    }
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Starting Balance Dialog
    if (showStartingBalanceDialog) {
        var balanceText by remember { mutableStateOf(if (startingBalance > 0L) (startingBalance / 100L).toString() else "") }
        AlertDialog(
            onDismissRequest = { showStartingBalanceDialog = false },
            title = { Text("Starting Available Balance") },
            text = {
                Column {
                    Text(
                        text = "Enter your current available balance (cash, bank, wallets). Used for calculating your daily safe-to-spend limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Available Cash / Balance") },
                        prefix = { Text("${Constants.CURRENCY_SYMBOL} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val paisa = CurrencyFormatter.parseRupeesToPaisa(balanceText) ?: 0L
                    scope.launch {
                        userPreferencesRepository.setStartingBalance(paisa)
                        showStartingBalanceDialog = false
                        snackbarHostState.showSnackbar("Available balance updated")
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartingBalanceDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Protected Reserve Dialog
    if (showProtectedReserveDialog) {
        var reserveText by remember { mutableStateOf(if (protectedReserve > 0L) (protectedReserve / 100L).toString() else "") }
        AlertDialog(
            onDismissRequest = { showProtectedReserveDialog = false },
            title = { Text("Protected Emergency Reserve") },
            text = {
                Column {
                    Text(
                        text = "Minimum emergency amount you wish to protect and never spend in your daily budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                    OutlinedTextField(
                        value = reserveText,
                        onValueChange = { reserveText = it },
                        label = { Text("Protected Reserve Amount") },
                        prefix = { Text("${Constants.CURRENCY_SYMBOL} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val paisa = CurrencyFormatter.parseRupeesToPaisa(reserveText) ?: 0L
                    scope.launch {
                        userPreferencesRepository.setProtectedReserve(paisa)
                        showProtectedReserveDialog = false
                        snackbarHostState.showSnackbar("Protected reserve updated")
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProtectedReserveDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
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
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            // General Preferences
            SettingsGroup(title = stringResource(R.string.settings_general)) {
                SettingsItem(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_language),
                    value = currentLanguage.displayName,
                    onClick = { showLanguageDialog = true },
                )
                SettingsItem(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_theme),
                    value = currentTheme.displayName,
                    onClick = { showThemeDialog = true },
                )
                SettingsItem(
                    icon = Icons.Filled.CurrencyExchange,
                    title = stringResource(R.string.settings_currency),
                    value = "PKR (Pakistani Rupee)",
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Pakistani Rupee (PKR) is the default currency.")
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Financial Planning & Reserves
            SettingsGroup(title = "Safe-to-Spend Parameters") {
                SettingsItem(
                    icon = Icons.Filled.AccountBalance,
                    title = "Starting Available Balance",
                    value = CurrencyFormatter.formatPaisa(startingBalance),
                    onClick = { showStartingBalanceDialog = true },
                )
                SettingsItem(
                    icon = Icons.Filled.Savings,
                    title = "Protected Emergency Reserve",
                    value = CurrencyFormatter.formatPaisa(protectedReserve),
                    onClick = { showProtectedReserveDialog = true },
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Data & Storage
            SettingsGroup(title = stringResource(R.string.settings_data)) {
                SettingsItem(
                    icon = Icons.Filled.PhoneAndroid,
                    title = stringResource(R.string.settings_storage),
                    value = "Local SQLite Room (Active)",
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("All transactions are securely stored locally on this device.")
                        }
                    },
                )
                SettingsItem(
                    icon = Icons.Filled.Cloud,
                    title = "Google Drive Backup",
                    value = "Coming Soon",
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Google Drive backup is in development and coming soon.")
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // About
            SettingsGroup(title = stringResource(R.string.settings_about)) {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "App Name",
                    value = "GharHisab",
                    onClick = {},
                )
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.settings_version),
                    value = Constants.APP_VERSION,
                    onClick = {},
                )
                SettingsItem(
                    icon = Icons.Filled.Shield,
                    title = "Developer",
                    value = Constants.COMPANY_NAME,
                    onClick = {},
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingHuge))
        }
    }
}
