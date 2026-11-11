package com.sindantechnologies.gharhisab.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.AppLockMode
import com.sindantechnologies.gharhisab.util.security.BiometricAuthManager
import kotlinx.coroutines.launch

@Composable
fun LockScreen(repository: UserPreferencesRepository, lockMode: AppLockMode, onUnlock: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var recovery by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val activity = com.sindantechnologies.gharhisab.util.security.findActivity(LocalContext.current)
    Surface(modifier = Modifier.fillMaxSize(), onClick = {}) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.settings_app_lock), style = MaterialTheme.typography.headlineMedium)
            if (lockMode == AppLockMode.PIN) {
                OutlinedTextField(input, { input = it }, label = { Text(stringResource(if (recovery) R.string.recovery_key else R.string.lock_pin)) },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = if (recovery) KeyboardType.Password else KeyboardType.NumberPassword))
                Button(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            if (repository.verifyAppPin(if (recovery) input.trim().uppercase() else input, recovery)) { input = ""; onUnlock() }
                            else { failed = true; input = "" }
                        } finally { busy = false }
                    }
                }) { Text(stringResource(R.string.lock_unlock)) }
                TextButton(onClick = { recovery = !recovery; input = ""; failed = false }) { Text(stringResource(R.string.lock_recover)) }
                if (recovery) Text(stringResource(R.string.recovery_explanation))
            } else {
                Button(onClick = { activity?.let { BiometricAuthManager.promptAuthentication(it, onSuccess = onUnlock, onError = { failed = true }) } }) { Text(stringResource(R.string.lock_unlock)) }
                Text(stringResource(R.string.device_lock_help))
                TextButton(onClick = { activity?.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)) }) { Text(stringResource(R.string.settings_title)) }
            }
            if (failed) Text(stringResource(R.string.lock_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}
