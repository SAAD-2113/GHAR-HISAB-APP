package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.data.backup.*
import com.sindantechnologies.gharhisab.ui.screens.backup.BackupViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

internal class FakeBackupOperations : BackupOperations {
    val value = BackupManager.Preview(BackupPayload(appVersion = "3.0.0", createdAtMillis = 1,
        startingBalancePaisa = 100, protectedReservePaisa = 0, categories = emptyList(),
        transactions = emptyList(), budgets = emptyList(), plannedExpenses = emptyList()), "snapshot")
    var previewFailure: Exception? = null
    var restoreFailure: Exception? = null
    var restores = 0
    var restoreGate: CompletableDeferred<Unit>? = null
    override suspend fun createEncryptedBackupBytes(password: String) = byteArrayOf(1, 2, 3)
    override suspend fun preview(bytes: ByteArray, password: String): BackupManager.Preview {
        previewFailure?.let { throw it }; return value
    }
    override suspend fun restore(preview: BackupManager.Preview): Int {
        restoreFailure?.let { throw it }; restores++; restoreGate?.await(); return 0
    }
}
internal class FakeDriveService : DriveService {
    var listFailure: Exception? = null
    var uploads = 0
    var uploadGate: CompletableDeferred<Unit>? = null
    override suspend fun list(): List<DriveBackupClient.RemoteBackup> {
        listFailure?.let { throw it }
        return listOf(DriveBackupClient.RemoteBackup("one", "GharHisab_one", "2026-11-11"))
    }
    override suspend fun upload(encrypted: ByteArray) { uploads++; uploadGate?.await() }
    override suspend fun download(id: String) = byteArrayOf(1, 2, 3)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DriveReliabilityTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val backup = FakeBackupOperations()
    private val drive = FakeDriveService()
    private val invalidated = mutableListOf<String>()
    private lateinit var model: BackupViewModel
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        model = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                BackupViewModel(backup, { drive }, { invalidated.add(it) }) as T
        })[BackupViewModel::class.java]
        model.authorized("test-token", null)
        dispatcher.scheduler.runCurrent()
    }
    @After fun close() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }

    @Test fun acknowledgedUploadRemainsSuccessfulWhenRefreshFails() = runTest(dispatcher) {
        drive.listFailure = java.io.IOException()
        model.upload("test password")
        runCurrent()
        assertEquals(1, drive.uploads)
        assertEquals(R.string.drive_upload_success, model.notice.message)
        assertEquals(R.string.drive_network_failed, model.error?.message)
        assertFalse(model.busy)
        drive.listFailure = null
        model.refresh(); runCurrent()
        assertNull(model.error)
        assertEquals(1, drive.uploads)
    }
    @Test fun repeatedTapCannotStartDuplicateUpload() = runTest(dispatcher) {
        drive.uploadGate = CompletableDeferred()
        model.upload("test password"); model.upload("test password")
        runCurrent()
        assertTrue(model.busy); assertEquals(1, drive.uploads)
        drive.uploadGate!!.complete(Unit); runCurrent()
        assertFalse(model.busy); assertEquals(R.string.drive_upload_success, model.notice.message)
    }
    @Test fun expiredAccessInvalidatesTokenButDoesNotEraseUploadAcknowledgement() = runTest(dispatcher) {
        drive.listFailure = DriveBackupClient.AuthorizationExpired()
        model.upload("test password"); runCurrent()
        assertEquals(R.string.drive_upload_success, model.notice.message)
        assertEquals(R.string.drive_expired, model.error?.message)
        assertEquals(listOf("test-token"), invalidated)
        assertFalse(model.connected)
    }
    @Test fun apiFailureKeepsOnlySafeStatusCode() = runTest(dispatcher) {
        drive.listFailure = DriveBackupClient.HttpFailure(403)
        model.refresh(); runCurrent()
        assertEquals(R.string.drive_http_failed, model.error?.message)
        assertEquals(403, model.error?.code)
        assertTrue(model.connected)
    }
    @Test fun wrongPasswordDoesNotBecomeAnOAuthErrorOrRestoreAnything() = runTest(dispatcher) {
        backup.previewFailure = BackupManager.DecryptionFailed()
        model.download("wrong password"); runCurrent()
        assertEquals(R.string.backup_decryption_failed, model.error?.message)
        assertTrue(model.connected); assertNull(model.preview); assertEquals(0, backup.restores)
    }
    @Test fun stalePreviewIsExplicitAndCannotSilentlyReplaceLocalRecords() = runTest(dispatcher) {
        model.localPreview(backup.value)
        backup.restoreFailure = BackupManager.StalePreview()
        model.restore(); runCurrent()
        assertEquals(R.string.backup_preview_stale, model.error?.message)
        assertNotNull(model.preview); assertEquals(0, backup.restores)
        model.dismissPreview(); assertNull(model.preview)
    }
    @Test fun restoreCompletesOnceAndRetainsSuccessWithoutUIObservers() = runTest(dispatcher) {
        backup.restoreGate = CompletableDeferred()
        model.localPreview(backup.value)
        model.restore(); model.restore(); runCurrent()
        assertTrue(model.busy); assertEquals(1, backup.restores)
        backup.restoreGate!!.complete(Unit); runCurrent()
        assertFalse(model.busy); assertNull(model.preview)
        assertEquals(R.string.backup_restored, model.notice.message)
        assertEquals(1, backup.restores)
    }
    @Test fun storageFailureIsNotMisreportedAsNetworkFailure() = runTest(dispatcher) {
        model.localPreview(backup.value)
        backup.restoreFailure = java.io.IOException()
        model.restore(); runCurrent()
        assertEquals(R.string.backup_restore_failed, model.error?.message)
        assertFalse(model.busy)
    }
}
