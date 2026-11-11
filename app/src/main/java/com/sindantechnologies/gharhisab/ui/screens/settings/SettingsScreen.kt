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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.backup.BackupManager
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
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Functional Settings screen with real persistent user configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferencesRepository: UserPreferencesRepository,
    backupManager: BackupManager,
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
    var showRollback by remember { mutableStateOf(false) }
    var rollbackBusy by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    var pendingSecurityAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var recoveryKey by remember { mutableStateOf<String?>(null) }
    val securityContext = LocalContext.current
    fun authenticate(action: () -> Unit) {
        when (currentLockMode) {
            AppLockMode.OFF -> action()
            AppLockMode.PIN -> pendingSecurityAction = action
            AppLockMode.BIOMETRIC -> com.sindantechnologies.gharhisab.util.security.findActivity(securityContext)?.let {
                BiometricAuthManager.promptAuthentication(it, onSuccess = action, onError = { scope.launch { snackbarHostState.showSnackbar(securityContext.getString(R.string.lock_failed)) } })
            }
        }
    }
    pendingSecurityAction?.let { action ->
        var pin by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var failed by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { pendingSecurityAction = null }, title = { Text(stringResource(R.string.lock_unlock)) },
            text = { Column {
                OutlinedTextField(pin, { pin = it }, visualTransformation = PasswordVisualTransformation(), label = { Text(stringResource(R.string.lock_pin)) })
                if (failed) Text(stringResource(R.string.lock_failed))
            } }, confirmButton = { Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        if (userPreferencesRepository.verifyAppPin(pin)) { pendingSecurityAction = null; action() }
                        else { failed = true; pin = "" }
                    } finally { busy = false }
                }
            }) { Text(stringResource(R.string.action_ok)) } })
    }
    recoveryKey?.let { key -> AlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.recovery_key)) },
        text = { Column { Text(stringResource(R.string.recovery_save)); androidx.compose.foundation.text.selection.SelectionContainer { Text(key) } } },
        confirmButton = { TextButton(onClick = { recoveryKey = null }) { Text(stringResource(R.string.recovery_saved)) } }) }

    if (showRollback) AlertDialog(onDismissRequest = { if (!rollbackBusy) showRollback = false }, title = { Text(stringResource(R.string.backup_rollback)) },
        text = { Text(stringResource(R.string.backup_replace_warning)) }, confirmButton = { TextButton(enabled = !rollbackBusy, onClick = {
            rollbackBusy = true
            scope.launch {
                try { backupManager.rollbackSafetyBackup(); snackbarHostState.showSnackbar(securityContext.getString(R.string.backup_restored)) }
                catch (_: Exception) { snackbarHostState.showSnackbar(securityContext.getString(R.string.operation_failed)) }
                showRollback = false
                rollbackBusy = false
            }
        }) { Text(stringResource(R.string.action_ok)) } }, dismissButton = { TextButton(enabled = !rollbackBusy, onClick = { showRollback = false }) { Text(stringResource(R.string.action_cancel)) } })

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
                                com.sindantechnologies.gharhisab.util.security.findActivity(context)?.let { activity ->
                                    BiometricAuthManager.promptAuthentication(activity, onSuccess = {
                                        scope.launch { userPreferencesRepository.setAppLockMode(AppLockMode.BIOMETRIC) }
                                    }, onError = { scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.lock_failed)) } })
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
                            recoveryKey = userPreferencesRepository.setAppLockPin(pin)
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

    var restorePreview by remember { mutableStateOf<com.sindantechnologies.gharhisab.data.backup.BackupManager.Preview?>(null) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showGoogleDriveInfoDialog by remember { mutableStateOf(false) }

    val hostActivity = com.sindantechnologies.gharhisab.util.security.findActivity(context) as? com.sindantechnologies.gharhisab.MainActivity
    androidx.compose.runtime.LaunchedEffect(hostActivity?.external?.googleResult, hostActivity?.external?.googleCancelled) {
        if (hostActivity?.external?.googleResult != null || hostActivity?.external?.googleCancelled == true) showGoogleDriveInfoDialog = true
    }
    androidx.compose.runtime.LaunchedEffect(hostActivity?.external?.importUri) {
        hostActivity?.external?.importUri?.let { selectedImportUri = it; hostActivity.external.importUri = null; showImportPasswordDialog = true }
    }
    androidx.compose.runtime.LaunchedEffect(hostActivity?.external?.exportUri) {
        val uri = hostActivity?.external?.exportUri
        if (uri != null) {
            hostActivity.external.exportUri = null
            val password = hostActivity.external.exportPassword
            hostActivity.external.exportPassword = ""
            try {
                val out = context.contentResolver.openOutputStream(uri) ?: error("Cannot open output")
                backupManager.exportToStream(out, password)
                snackbarHostState.showSnackbar(context.getString(R.string.backup_exported))
            } catch (_: Exception) { snackbarHostState.showSnackbar(context.getString(R.string.operation_failed)) }
        }
    }

    // Export Password Dialog
    if (showExportPasswordDialog) {
        var exportPassword by remember { mutableStateOf("") }
        var exportPasswordConfirm by remember { mutableStateOf("") }
        var exportError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = { Text("Encrypt & Export Backup") },
            text = {
                Column {
                    Text(
                        text = "Choose a strong password to encrypt your financial backup using AES-256-GCM. You will need this password to restore your data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it; exportError = null },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    OutlinedTextField(
                        value = exportPasswordConfirm,
                        onValueChange = { exportPasswordConfirm = it; exportError = null },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = exportError != null,
                        supportingText = exportError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (exportPassword.length < 12) {
                        exportError = "Password must be at least 12 characters"
                    } else if (exportPassword != exportPasswordConfirm) {
                        exportError = "Passwords do not match"
                    } else {
                        hostActivity?.external?.exportPassword = exportPassword
                        showExportPasswordDialog = false
                        val fileName = "GharHisab_Backup_${SimpleDateFormat("yyyyMMdd_HHmm").format(Date())}.gharhisab"
                        hostActivity?.exportDocument?.launch(fileName)
                    }
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Import Password Dialog
    if (showImportPasswordDialog) {
        var importPassword by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }
        var isRestoring by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isRestoring) showImportPasswordDialog = false },
            title = { Text("Decrypt & Restore Backup") },
            text = {
                Column {
                    Text(
                        text = "Enter the password used when creating this backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it; importError = null },
                        label = { Text("Decryption Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = importError != null,
                        supportingText = importError?.let { { Text(it) } },
                        enabled = !isRestoring,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedImportUri ?: return@Button
                        isRestoring = true
                        scope.launch {
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                if (inputStream != null) {
                                    restorePreview = backupManager.preview(backupManager.readBounded(inputStream), importPassword)
                                    showImportPasswordDialog = false
                                } else {
                                    importError = "Cannot open selected file"
                                }
                            } catch (e: Exception) {
                                importError = "Restore error: ${e.localizedMessage}"
                            } finally {
                                isRestoring = false
                            }
                        }
                    },
                    enabled = !isRestoring
                ) {
                    Text(if (isRestoring) "Restoring..." else "Restore Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportPasswordDialog = false },
                    enabled = !isRestoring
                ) { Text("Cancel") }
            }
        )
    }

    if (showGoogleDriveInfoDialog) {
        com.sindantechnologies.gharhisab.ui.screens.backup.DriveBackupPanel(backupManager) { showGoogleDriveInfoDialog = false }
    }
    restorePreview?.let { preview ->
        com.sindantechnologies.gharhisab.ui.screens.backup.RestorePreviewDialog(preview, backupManager,
            onDismiss = { restorePreview = null }, onRestored = { restorePreview = null; showImportPasswordDialog = false })
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
                    onClick = { authenticate { showLockModeDialog = true } },
                )
                if (currentLockMode != AppLockMode.OFF) {
                    SettingsItem(
                        icon = Icons.Filled.Shield,
                        title = "Auto-lock Timeout",
                        value = currentLockTimeout.label,
                        onClick = { authenticate { showTimeoutDialog = true } },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Data & Storage
            SettingsGroup(title = stringResource(R.string.settings_data)) {
                SettingsItem(icon = Icons.Filled.CloudDownload, title = stringResource(R.string.backup_rollback), value = "", onClick = { authenticate { showRollback = true } })
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
                    title = "Google Drive Cloud Sync",
                    value = stringResource(R.string.drive_model_short),
                    onClick = { showGoogleDriveInfoDialog = true },
                )
                SettingsItem(
                    icon = Icons.Filled.CloudUpload,
                    title = "Export Encrypted Backup",
                    value = "AES-256-GCM (.gharhisab)",
                    onClick = { showExportPasswordDialog = true },
                )
                SettingsItem(
                    icon = Icons.Filled.CloudDownload,
                    title = "Restore from Encrypted Backup",
                    value = "Import file",
                    onClick = { hostActivity?.importDocument?.launch(arrayOf("*/*")) },
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
