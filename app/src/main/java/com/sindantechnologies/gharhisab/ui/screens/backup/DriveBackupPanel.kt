package com.sindantechnologies.gharhisab.ui.screens.backup

import androidx.activity.result.IntentSenderRequest
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
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.sindantechnologies.gharhisab.ui.theme.appOutlinedTextFieldColors
import com.sindantechnologies.gharhisab.ui.theme.AppFilterChip as FilterChip
import com.sindantechnologies.gharhisab.ui.theme.AppTextButton as TextButton
import com.sindantechnologies.gharhisab.R

@Composable
fun BackupNotice(notice: BackupViewModel.Notice, isError: Boolean = false) {
    Text(stringResource(notice.message), color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    notice.code?.let { Text(stringResource(R.string.backup_error_code, it)) }
}

@Composable
fun DriveBackupPanel(model: BackupViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val client = remember { Identity.getAuthorizationClient(context.applicationContext) }
    val scopes = remember { listOf(Scope("https://www.googleapis.com/auth/drive.appdata")) }
    // Password inputs are deliberately not saved across screen recreation.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    fun failed(error: Exception) {
        val code = (error as? ApiException)?.statusCode
        model.authorizationFailed(code, cancelled = code == com.google.android.gms.common.api.CommonStatusCodes.CANCELED)
    }
    fun accept(result: AuthorizationResult) {
        if (result.hasResolution()) {
            result.pendingIntent?.let(model::resolveAuthorization) ?: model.authorizationFailed()
        } else {
            val token = result.accessToken
            if (token == null || !result.grantedScopes.containsAll(scopes.map { it.scopeUri })) model.authorizationFailed()
            else model.authorized(token, result.toGoogleSignInAccount()?.account)
        }
    }
    val host = com.sindantechnologies.gharhisab.util.security.findActivity(context) as? com.sindantechnologies.gharhisab.MainActivity
    LaunchedEffect(model.authorizationResolution) {
        val pending = model.authorizationResolution ?: return@LaunchedEffect
        if (host == null) { model.authorizationFailed(); return@LaunchedEffect }
        model.resolutionLaunched()
        try { host.googleAuthorization.launch(IntentSenderRequest.Builder(pending.intentSender).build()) }
        catch (e: Exception) { failed(e) }
    }
    LaunchedEffect(host?.external?.googleResult) {
        val result = host?.external?.googleResult ?: return@LaunchedEffect
        host.external.googleResult = null
        when (val decoded = com.sindantechnologies.gharhisab.data.backup.GoogleAuthorizationOutcome.decode(result.data, client::getAuthorizationResultFromIntent)) {
            is com.sindantechnologies.gharhisab.data.backup.GoogleAuthorizationOutcome.Result.Success -> accept(decoded.value)
            is com.sindantechnologies.gharhisab.data.backup.GoogleAuthorizationOutcome.Result.Failure -> model.authorizationFailed(decoded.code, decoded.userCancelled)
        }
    }
    AlertDialog(onDismissRequest = { if (!model.busy) onDismiss() }, title = { Text(stringResource(R.string.drive_title)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.drive_model))
            BackupNotice(model.notice)
            model.error?.let {
                BackupNotice(it, isError = true)
                if (it.message == R.string.drive_authorization_failed) {
                    Text(stringResource(R.string.drive_oauth_certificate_help), style = MaterialTheme.typography.bodySmall)
                    val certificate = remember { com.sindantechnologies.gharhisab.util.SigningIdentity.sha1(context) }
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Column { Text(context.packageName); certificate?.let { fingerprint -> Text(fingerprint, style = MaterialTheme.typography.bodySmall) } }
                    }
                }
            }
            model.account?.name?.let { Text(it) }
            if (!model.connected) Button(enabled = !model.busy, onClick = {
                if (model.beginAuthorization()) {
                    try {
                        client.authorize(AuthorizationRequest.builder().setRequestedScopes(scopes).build())
                            .addOnSuccessListener { accept(it) }.addOnFailureListener { failed(it) }
                            .addOnCanceledListener { model.authorizationFailed(cancelled = true) }
                    } catch (e: Exception) { failed(e) }
                }
            }) { Text(stringResource(R.string.drive_connect)) }
            else {
                OutlinedTextField(password, { password = it }, enabled = !model.busy, label = { Text(stringResource(R.string.backup_password)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), colors = appOutlinedTextFieldColors())
                OutlinedTextField(confirmation, { confirmation = it }, enabled = !model.busy, label = { Text(stringResource(R.string.backup_confirm_password)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), colors = appOutlinedTextFieldColors())
                Text(stringResource(R.string.backup_recovery_warning))
                Button(enabled = !model.busy && password.length >= 12 && password == confirmation, onClick = {
                    model.upload(password); password = ""; confirmation = ""
                }) { Text(stringResource(R.string.drive_backup_now)) }
                TextButton(enabled = !model.busy, onClick = model::refresh) { Text(stringResource(R.string.drive_sync_now)) }
                model.backups.firstOrNull()?.let { Text(stringResource(R.string.drive_last_backup, it.modifiedTime)) }
                model.backups.forEach { backup ->
                    FilterChip(model.selected == backup, { model.select(backup) }, enabled = !model.busy, label = { Text(backup.modifiedTime) })
                }
                Button(enabled = !model.busy && model.selected != null && password.isNotEmpty(), onClick = {
                    model.download(password); password = ""; confirmation = ""
                }) { Text(stringResource(R.string.backup_preview)) }
                TextButton(enabled = !model.busy, onClick = {
                    val current = model.account
                    if (current == null) model.authorizationFailed()
                    else if (model.beginAuthorization()) {
                        try {
                            client.revokeAccess(RevokeAccessRequest.builder().setAccount(current).setScopes(scopes).build())
                                .addOnSuccessListener { model.disconnected() }.addOnFailureListener { failed(it) }
                                .addOnCanceledListener { model.authorizationFailed(cancelled = true) }
                        } catch (e: Exception) { failed(e) }
                    }
                }) { Text(stringResource(R.string.drive_disconnect)) }
            }
            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = onDismiss, enabled = !model.busy) { Text(stringResource(R.string.action_back)) } })
}

@Composable
fun RestorePreviewDialog(model: BackupViewModel) {
    val preview = model.preview ?: return
    AlertDialog(onDismissRequest = model::dismissPreview, title = { Text(stringResource(R.string.backup_preview)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.backup_preview_details, preview.payload.transactions.size, preview.payload.plannedExpenses.size, java.util.Date(preview.payload.createdAtMillis).toString()))
            Text(stringResource(R.string.backup_replace_warning))
            model.error?.let { BackupNotice(it, isError = true) }
            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { Button(enabled = !model.busy, onClick = model::restore) { Text(stringResource(R.string.backup_restore)) } },
        dismissButton = { TextButton(enabled = !model.busy, onClick = model::dismissPreview) { Text(stringResource(R.string.action_cancel)) } })
}
