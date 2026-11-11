package com.sindantechnologies.gharhisab.data.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-bound rollback archive, deliberately separate from portable password-encrypted backups. */
interface SafetyVault {
    fun writePending(bytes: ByteArray)
    fun readPending(): ByteArray?
    fun complete()
    fun readSafety(): ByteArray?
}
class KeystoreSafetyVault(context: Context) : SafetyVault {
    private val directory = File(context.noBackupFilesDir, "restore-safety").apply { mkdirs() }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("gharhisab.restore", null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("gharhisab.restore", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    }
    override fun writePending(bytes: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val temp = File(directory, "pending.tmp")
        java.io.FileOutputStream(temp).use { it.write(cipher.iv + cipher.doFinal(bytes)); it.fd.sync() }
        check(temp.renameTo(File(directory, "pending")))
    }
    private fun read(name: String): ByteArray? {
        val file = File(directory, name)
        if (!file.exists()) return null
        val bytes = file.readBytes()
        require(bytes.size >= 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size))
    }
    override fun readPending() = read("pending")
    override fun readSafety() = read("safety")
    override fun complete() { check(File(directory, "pending").renameTo(File(directory, "safety"))) }
}
