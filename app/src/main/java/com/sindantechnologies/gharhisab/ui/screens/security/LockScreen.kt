package com.sindantechnologies.gharhisab.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.sindantechnologies.gharhisab.ui.theme.appOutlinedTextFieldColors
import com.sindantechnologies.gharhisab.ui.theme.AppTextButton as TextButton
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.AppLockMode
import com.sindantechnologies.gharhisab.util.security.BiometricAuthManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LockScreen(repository: UserPreferencesRepository, lockMode: AppLockMode, onUnlock: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var recovery by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = com.sindantechnologies.gharhisab.util.security.findActivity(context)
    val deviceState = remember(activity) { activity?.let(BiometricAuthManager::state) }
    val deviceBusy = deviceState?.inProgress == true
    val working = busy || deviceBusy
    val welcome = stringResource(R.string.security_welcome)
    val deviceSubtitle = stringResource(R.string.security_device_unlock)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.padding(22.dp).size(42.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(24.dp))
            Text(welcome, style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.security_unlock_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
            Card(Modifier.widthIn(max = 420.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (lockMode == AppLockMode.PIN) {
                        Text(stringResource(if (recovery) R.string.recovery_key else R.string.security_pin_help), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(input, { value ->
                            input = if (recovery) value.take(80) else com.sindantechnologies.gharhisab.util.security.SecurityUtils.normalizePinDigits(value).filter { it in '0'..'9' }.take(4)
                            failed = false
                        }, enabled = !working, singleLine = true,
                            label = { Text(stringResource(if (recovery) R.string.recovery_key else R.string.lock_pin)) },
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = if (recovery) KeyboardType.Password else KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(), isError = failed, colors = appOutlinedTextFieldColors())
                        Button(enabled = !working && if (recovery) input.isNotBlank() else input.length == 4, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
                            busy = true; failed = false
                            scope.launch {
                                try {
                                    if (repository.verifyAppPin(input, recovery)) { input = ""; onUnlock() }
                                    else { failed = true; input = "" }
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) { failed = true }
                                finally { busy = false }
                            }
                        }) {
                            if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                            Text(stringResource(if (busy) R.string.security_verifying else R.string.lock_unlock))
                        }
                        TextButton(enabled = !working, onClick = { recovery = !recovery; input = ""; failed = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(stringResource(if (recovery) R.string.security_use_pin else R.string.security_recovery_action))
                        }
                        if (recovery) {
                            Text(stringResource(R.string.security_recovery_help), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.security_recovery_missing), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Button(enabled = !working, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = {
                            failed = false
                            activity?.let { BiometricAuthManager.promptAuthentication(it, title = welcome, subtitle = deviceSubtitle,
                                onSuccess = onUnlock, onError = { failed = true }, onNonMatch = { failed = true }) }
                        }) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(if (deviceBusy) R.string.security_verifying else R.string.security_device_unlock))
                        }
                        Text(stringResource(R.string.device_lock_help), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { activity?.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)) }) { Text(stringResource(R.string.settings_title)) }
                    }
                    if (failed || (lockMode == AppLockMode.BIOMETRIC && deviceState?.failed == true))
                        Text(stringResource(R.string.lock_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
