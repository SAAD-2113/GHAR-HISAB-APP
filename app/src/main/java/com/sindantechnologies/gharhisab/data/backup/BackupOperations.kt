package com.sindantechnologies.gharhisab.data.backup

/** Operations shared by the retained UI controller and its isolated tests. */
interface BackupOperations {
    suspend fun createEncryptedBackupBytes(password: String): ByteArray
    suspend fun preview(bytes: ByteArray, password: String): BackupManager.Preview
    suspend fun restore(preview: BackupManager.Preview): Int
}

interface DriveService {
    suspend fun list(): List<DriveBackupClient.RemoteBackup>
    suspend fun upload(encrypted: ByteArray)
    suspend fun download(id: String): ByteArray
}
