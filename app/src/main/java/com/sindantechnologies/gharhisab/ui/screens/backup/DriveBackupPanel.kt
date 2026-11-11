package com.sindantechnologies.gharhisab.ui.screens.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.*
import com.google.android.gms.common.api.Scope
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.backup.*
import kotlinx.coroutines.launch

@Composable
fun DriveBackupPanel(manager: BackupManager, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val client = remember { Identity.getAuthorizationClient(context) }
    val scopes = remember { listOf(Scope("https://www.googleapis.com/auth/drive.appdata")) }
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf<String?>(null) }
    var account by remember { mutableStateOf<android.accounts.Account?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(R.string.drive_connect_help) }
    var backups by remember { mutableStateOf<List<DriveBackupClient.RemoteBackup>>(emptyList()) }
    var selected by remember { mutableStateOf<DriveBackupClient.RemoteBackup?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<BackupManager.Preview?>(null) }
    fun failure(error: Exception) {
        status = if (error is DriveBackupClient.AuthorizationExpired) R.string.drive_expired else R.string.drive_failed
        if (error is DriveBackupClient.AuthorizationExpired) {
            token?.let { client.clearToken(ClearTokenRequest.builder().setToken(it).build()) }
            token = null; account = null; backups = emptyList(); selected = null
        }
    }
    fun refresh(access: String) {
        scope.launch {
            busy = true
            try {
                backups = DriveBackupClient(access).list()
                selected = backups.firstOrNull()
                status = if (backups.isEmpty()) R.string.drive_no_backup else R.string.drive_connected
            } catch (e: Exception) { failure(e) } finally { busy = false }
        }
    }
    fun accept(result: AuthorizationResult) {
        token = result.accessToken
        account = result.toGoogleSignInAccount()?.account
        val access = token
        if (access == null) { status = R.string.drive_failed; busy = false }
        else refresh(access)
    }
    val host = com.sindantechnologies.gharhisab.util.security.findActivity(context) as? com.sindantechnologies.gharhisab.MainActivity
    LaunchedEffect(host?.external?.googleResult, host?.external?.googleCancelled) {
        val result = host?.external?.googleResult
        if (result != null) {
            host.external.googleResult = null
            try { accept(client.getAuthorizationResultFromIntent(result)) }
            catch (_: Exception) { busy = false; status = R.string.drive_failed }
        } else if (host?.external?.googleCancelled == true) {
            host.external.googleCancelled = false; status = R.string.drive_cancelled; busy = false
        }
    }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(stringResource(R.string.drive_title)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.drive_model))
            Text(stringResource(status))
            account?.name?.let { Text(it) }
            if (token == null) Button(enabled = !busy, onClick = {
                busy = true
                client.authorize(AuthorizationRequest.builder().setRequestedScopes(scopes).build())
                    .addOnSuccessListener { result ->
                        if (result.hasResolution()) host?.googleAuthorization?.launch(IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build())
                        else accept(result)
                    }.addOnFailureListener { busy = false; status = R.string.drive_failed }
            }) { Text(stringResource(R.string.drive_connect)) }
            else {
                OutlinedTextField(password, { password = it }, enabled = !busy, label = { Text(stringResource(R.string.backup_password)) }, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(confirmation, { confirmation = it }, enabled = !busy, label = { Text(stringResource(R.string.backup_confirm_password)) }, visualTransformation = PasswordVisualTransformation())
                Text(stringResource(R.string.backup_recovery_warning))
                Button(enabled = !busy && password.length >= 12 && password == confirmation, onClick = {
                    val access = token ?: return@Button
                    busy = true
                    scope.launch {
                        try {
                            DriveBackupClient(access).upload(manager.createEncryptedBackupBytes(password))
                            password = ""; confirmation = ""
                            backups = DriveBackupClient(access).list(); selected = backups.firstOrNull()
                            status = R.string.drive_upload_success
                        } catch (e: Exception) { failure(e) } finally { busy = false }
                    }
                }) { Text(stringResource(R.string.drive_backup_now)) }
                TextButton(enabled = !busy, onClick = { token?.let(::refresh) }) { Text(stringResource(R.string.drive_sync_now)) }
                backups.firstOrNull()?.let { Text(stringResource(R.string.drive_last_backup, it.modifiedTime)) }
                backups.forEach { backup ->
                    FilterChip(selected == backup, { selected = backup }, enabled = !busy, label = { Text(backup.modifiedTime) })
                }
                Button(enabled = !busy && selected != null && password.isNotEmpty(), onClick = {
                    val access = token ?: return@Button
                    val backup = selected ?: return@Button
                    busy = true
                    scope.launch {
                        try { preview = manager.preview(DriveBackupClient(access).download(backup.id), password); password = ""; confirmation = "" }
                        catch (e: Exception) { failure(e) } finally { busy = false }
                    }
                }) { Text(stringResource(R.string.backup_preview)) }
                TextButton(enabled = !busy, onClick = {
                    val current = account
                    if (current == null) { token = null; backups = emptyList(); selected = null }
                    else {
                        busy = true
                        client.revokeAccess(RevokeAccessRequest.builder().setAccount(current).setScopes(scopes).build())
                            .addOnSuccessListener { token = null; account = null; password = ""; confirmation = ""; backups = emptyList(); selected = null; busy = false; status = R.string.drive_connect_help }
                            .addOnFailureListener { busy = false; status = R.string.drive_failed }
                    }
                }) { Text(stringResource(R.string.drive_disconnect)) }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.action_back)) } })
    preview?.let { value -> RestorePreviewDialog(value, manager, { preview = null }) { status = R.string.backup_restored; preview = null } }
}

@Composable
fun RestorePreviewDialog(preview: BackupManager.Preview, manager: BackupManager, onDismiss: () -> Unit, onRestored: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(stringResource(R.string.backup_preview)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.backup_preview_details, preview.payload.transactions.size, preview.payload.plannedExpenses.size, java.util.Date(preview.payload.createdAtMillis).toString()))
            Text(stringResource(R.string.backup_replace_warning))
            if (failed) Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { Button(enabled = !busy, onClick = {
        busy = true
        scope.launch {
            try { manager.restore(preview); onRestored() }
            catch (_: Exception) { failed = true }
            finally { busy = false }
        }
    }) { Text(stringResource(R.string.backup_restore)) } }, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } })
}
