package com.sindantechnologies.gharhisab.ui.screens.backup

import android.accounts.Account
import android.app.PendingIntent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.backup.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Activity-owned, memory-only operation state. No credentials or plaintext enter saved state. */
class BackupViewModel(
    private val manager: BackupOperations,
    private val drive: (String) -> DriveService = { DriveBackupClient(it) },
    private val invalidateToken: suspend (String) -> Unit = {},
) : ViewModel() {
    data class Notice(val message: Int, val code: Int? = null)
    private enum class Stage { CREATE, UPLOAD, LIST, DOWNLOAD, DECRYPT, RESTORE }
    var driveOpen by mutableStateOf(false)
    var busy by mutableStateOf(false); private set
    var connected by mutableStateOf(false); private set
    var account by mutableStateOf<Account?>(null); private set
    var notice by mutableStateOf(Notice(R.string.drive_connect_help)); private set
    var error by mutableStateOf<Notice?>(null); private set
    var backups by mutableStateOf<List<DriveBackupClient.RemoteBackup>>(emptyList()); private set
    var selected by mutableStateOf<DriveBackupClient.RemoteBackup?>(null); private set
    var preview by mutableStateOf<BackupManager.Preview?>(null); private set
    var authorizationResolution by mutableStateOf<PendingIntent?>(null); private set
    private var accessToken: String? = null

    fun select(value: DriveBackupClient.RemoteBackup) { if (!busy) selected = value }
    fun beginAuthorization(): Boolean {
        if (busy) return false
        busy = true; error = null
        return true
    }
    fun resolveAuthorization(pending: PendingIntent) { authorizationResolution = pending }
    fun resolutionLaunched() { authorizationResolution = null }
    fun authorizationFailed(code: Int? = null, cancelled: Boolean = false) {
        authorizationResolution = null; busy = false
        error = if (cancelled) null else Notice(R.string.drive_authorization_failed, code)
        notice = Notice(if (cancelled) R.string.drive_cancelled else R.string.drive_connect_help)
    }
    fun authorized(token: String, user: Account?) {
        accessToken = token; account = user; connected = true; busy = false
        backups = emptyList(); selected = null
        refresh()
    }
    fun disconnected() {
        accessToken = null; account = null; connected = false; busy = false
        backups = emptyList(); selected = null; preview = null; error = null
        notice = Notice(R.string.drive_connect_help)
    }
    private fun client(): DriveService = drive(checkNotNull(accessToken))
    private suspend fun list(client: DriveService) {
        val items = client.list()
        backups = items
        selected = items.firstOrNull { it.id == selected?.id } ?: items.firstOrNull()
    }
    private fun launchOperation(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            finally { busy = false }
        }
    }
    private suspend fun failed(failure: Exception, stage: Stage) {
        if (failure is CancellationException) throw failure
        error = when (failure) {
            is DriveBackupClient.AuthorizationExpired -> Notice(R.string.drive_expired)
            is DriveBackupClient.HttpFailure -> Notice(R.string.drive_http_failed, failure.statusCode)
            is DriveBackupClient.ResponseTooLarge -> Notice(R.string.backup_file_too_large)
            is BackupManager.DecryptionFailed -> Notice(R.string.backup_decryption_failed)
            is BackupManager.InvalidBackup -> Notice(R.string.backup_invalid)
            is BackupManager.StalePreview -> Notice(R.string.backup_preview_stale)
            is java.io.IOException -> Notice(when (stage) {
                Stage.UPLOAD -> R.string.drive_upload_unconfirmed
                Stage.LIST, Stage.DOWNLOAD -> R.string.drive_network_failed
                Stage.RESTORE -> R.string.backup_restore_failed
                else -> R.string.backup_creation_failed
            })
            else -> Notice(when (stage) {
                Stage.CREATE -> R.string.backup_creation_failed
                Stage.LIST -> R.string.drive_listing_failed
                Stage.UPLOAD -> R.string.drive_upload_unconfirmed
                Stage.DOWNLOAD -> R.string.drive_download_failed
                Stage.DECRYPT -> R.string.backup_invalid
                Stage.RESTORE -> R.string.backup_restore_failed
            })
        }
        if (notice.message != R.string.drive_upload_success) notice = Notice(R.string.backup_operation_stopped)
        if (failure is DriveBackupClient.AuthorizationExpired) {
            val expired = accessToken
            accessToken = null; connected = false; account = null; backups = emptyList(); selected = null
            if (expired != null) {
                try { invalidateToken(expired) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = Notice(R.string.drive_authorization_failed) }
            }
        }
    }
    fun refresh() {
        if (!connected) return
        launchOperation {
            notice = Notice(R.string.drive_refreshing)
            try {
                list(client())
                notice = Notice(if (backups.isEmpty()) R.string.drive_no_backup else R.string.drive_connected)
            } catch (e: Exception) { failed(e, Stage.LIST) }
        }
    }
    fun upload(password: String) {
        if (!connected) return
        launchOperation {
            notice = Notice(R.string.drive_uploading)
            val bytes = try { manager.createEncryptedBackupBytes(password) }
            catch (e: Exception) { failed(e, Stage.CREATE); return@launchOperation }
            val service = client()
            try { service.upload(bytes) }
            catch (e: Exception) { failed(e, Stage.UPLOAD); return@launchOperation }
            // Upload is acknowledged. A subsequent refresh cannot turn it into an upload failure.
            notice = Notice(R.string.drive_upload_success)
            try { list(service) }
            catch (e: Exception) { failed(e, Stage.LIST) }
        }
    }
    fun download(password: String) {
        val backup = selected ?: return
        if (!connected) return
        launchOperation {
            notice = Notice(R.string.drive_downloading)
            val bytes = try { client().download(backup.id) }
            catch (e: Exception) { failed(e, Stage.DOWNLOAD); return@launchOperation }
            try { preview = manager.preview(bytes, password); notice = Notice(R.string.backup_preview) }
            catch (e: Exception) { failed(e, Stage.DECRYPT) }
        }
    }
    fun localPreview(value: BackupManager.Preview) {
        if (!busy) { preview = value; error = null }
    }
    fun dismissPreview() { if (!busy) { preview = null; error = null } }
    fun restore() {
        val value = preview ?: return
        launchOperation {
            notice = Notice(R.string.backup_restoring)
            try { manager.restore(value); preview = null; notice = Notice(R.string.backup_restored) }
            catch (e: Exception) { failed(e, Stage.RESTORE) }
        }
    }
}
