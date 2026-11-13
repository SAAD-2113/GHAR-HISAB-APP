package com.sindantechnologies.gharhisab

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.AppLockMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Memory-only pending results/settings; secrets never enter saved-state bundles. */
class ExternalActions : ViewModel() {
    data class GoogleResult(val resultCode: Int, val data: Intent?)
    var googleResult by mutableStateOf<GoogleResult?>(null)
    fun recordGoogleResult(code: Int, data: Intent?) { googleResult = GoogleResult(code, data) }
    var importUri by mutableStateOf<Uri?>(null)
    var exportUri by mutableStateOf<Uri?>(null)
    var exportPassword by mutableStateOf("")
    var showLockModeDialog by mutableStateOf(false)
    var showPinSetupDialog by mutableStateOf(false)
    var showTimeoutDialog by mutableStateOf(false)
    var showRollback by mutableStateOf(false)
    var rollbackBusy by mutableStateOf(false); private set
    var rollbackNotice by mutableStateOf<Int?>(null); private set
    fun dismissRollbackNotice() { rollbackNotice = null }
    fun rollback(action: suspend () -> Unit) {
        if (rollbackBusy) return
        rollbackBusy = true; rollbackNotice = null
        viewModelScope.launch {
            try { action(); rollbackNotice = R.string.backup_restored }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { rollbackNotice = R.string.backup_restore_failed }
            finally { rollbackBusy = false; showRollback = false }
        }
    }
    var recoveryKey by mutableStateOf<String?>(null)
    var securityBusy by mutableStateOf(false); private set
    var securityFailed by mutableStateOf(false); private set

    fun reportSecurityFailure() { securityFailed = true }
    fun dismissSecurityFailure() { securityFailed = false }

    fun enableDeviceLock(repository: UserPreferencesRepository) = securityOperation {
        repository.setAppLockMode(AppLockMode.BIOMETRIC)
    }
    fun createPin(repository: UserPreferencesRepository, pin: String) = securityOperation {
        recoveryKey = repository.setAppLockPin(pin)
        showPinSetupDialog = false
    }
    private fun securityOperation(action: suspend () -> Unit) {
        if (securityBusy) return
        securityBusy = true; securityFailed = false
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { securityFailed = true }
            finally { securityBusy = false }
        }
    }
}
