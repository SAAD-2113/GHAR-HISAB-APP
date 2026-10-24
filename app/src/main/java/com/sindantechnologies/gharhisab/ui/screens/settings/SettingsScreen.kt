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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.AppLockMode
import com.sindantechnologies.gharhisab.domain.model.AppLockTimeout
import com.sindantechnologies.gharhisab.domain.model.Language
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.util.security.BiometricAuthManager
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
    val currentLockMode by userPreferencesRepository.appLockModeFlow.collectAsStateWithLifecycle(initialValue = AppLockMode.OFF)
    val currentLockTimeout by userPreferencesRepository.appLockTimeoutFlow.collectAsStateWithLifecycle(initialValue = AppLockTimeout.IMMEDIATE)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showStartingBalanceDialog by remember { mutableStateOf(false) }
    var showProtectedReserveDialog by remember { mutableStateOf(false) }
    var showLockModeDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

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

    val context = LocalContext.current

    // App Lock Mode Dialog
    if (showLockModeDialog) {
        var tempMode by remember { mutableStateOf(currentLockMode) }
        AlertDialog(
            onDismissRequest = { showLockModeDialog = false },
            title = { Text("Application Security") },
            text = {
                Column {
                    AppLockMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = tempMode == mode,
                                    onClick = { tempMode = mode }
                                )
                                .padding(vertical = Dimens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tempMode == mode,
                                onClick = { tempMode = mode }
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text(mode.title)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showLockModeDialog = false
                    when (tempMode) {
                        AppLockMode.OFF -> {
                            scope.launch {
                                userPreferencesRepository.clearAppLock()
                                snackbarHostState.showSnackbar("App lock disabled")
                            }
                        }
                        AppLockMode.PIN -> {
                            showPinSetupDialog = true
                        }
                        AppLockMode.BIOMETRIC -> {
                            if (BiometricAuthManager.canAuthenticate(context)) {
                                scope.launch {
                                    userPreferencesRepository.setAppLockMode(AppLockMode.BIOMETRIC)
                                    snackbarHostState.showSnackbar("Biometric / Device lock enabled")
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Biometric or device lock is not configured on this phone")
                                }
                            }
                        }
                    }
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockModeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("Enter 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError != null,
                        supportingText = pinError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pin.length != 4) {
                        pinError = "PIN must be exactly 4 digits"
                    } else if (pin != confirmPin) {
                        pinError = "PINs do not match"
                    } else {
                        scope.launch {
                            userPreferencesRepository.setAppLockPin(pin)
                            showPinSetupDialog = false
                            snackbarHostState.showSnackbar("Security PIN set successfully")
                        }
                    }
                }) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Auto-lock Timeout Dialog
    if (showTimeoutDialog) {
        var tempTimeout by remember { mutableStateOf(currentLockTimeout) }
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Auto-lock Timeout") },
            text = {
                Column {
                    AppLockTimeout.entries.forEach { timeout ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = tempTimeout == timeout,
                                    onClick = { tempTimeout = timeout }
                                )
                                .padding(vertical = Dimens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tempTimeout == timeout,
                                onClick = { tempTimeout = timeout }
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text(timeout.label)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        userPreferencesRepository.setAppLockTimeout(tempTimeout)
                        showTimeoutDialog = false
                        snackbarHostState.showSnackbar("Auto-lock timeout updated")
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeoutDialog = false }) { Text("Cancel") }
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

            // Security & Privacy
            SettingsGroup(title = "Security & Privacy") {
                SettingsItem(
                    icon = if (currentLockMode == AppLockMode.BIOMETRIC) Icons.Filled.Fingerprint else Icons.Filled.Lock,
                    title = "Application Lock",
                    value = currentLockMode.title,
                    onClick = { showLockModeDialog = true },
                )
                if (currentLockMode != AppLockMode.OFF) {
                    SettingsItem(
                        icon = Icons.Filled.Shield,
                        title = "Auto-lock Timeout",
                        value = currentLockTimeout.label,
                        onClick = { showTimeoutDialog = true },
                    )
                }
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
