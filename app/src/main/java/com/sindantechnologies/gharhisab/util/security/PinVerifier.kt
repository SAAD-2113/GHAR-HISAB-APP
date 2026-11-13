package com.sindantechnologies.gharhisab.util.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/** Device-bound PIN verifier. The key is non-exportable; application rate limiting is still required. */
interface PinVerifier {
    fun create(pin: String, salt: ByteArray): String
    fun matches(pin: String, salt: ByteArray, expected: String): Boolean
}

class KeystorePinVerifier : PinVerifier {
    companion object {
        const val PREFIX = "v3k:"
        private val creationLock = Any()
        private const val ALIAS = "gharhisab.pin.v3"
    }
    private fun existingKey(): SecretKey? = KeyStore.getInstance("AndroidKeyStore").run {
        load(null)
        getKey(ALIAS, null) as? SecretKey
    }
    private fun keyForCreation(): SecretKey = synchronized(creationLock) {
        existingKey() ?: KeyGenerator.getInstance("HmacSHA256", "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256).setKeySize(256)
                .setUserAuthenticationRequired(false).build())
            generateKey()
        }
    }
    private fun digest(pin: String, salt: ByteArray, key: SecretKey): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(key)
        update("GharHisab app PIN v3\u0000".toByteArray(Charsets.UTF_8))
        update(salt)
        doFinal(pin.toByteArray(Charsets.UTF_8))
    }
    override fun create(pin: String, salt: ByteArray): String = PREFIX + SecurityUtils.bytesToHex(digest(pin, salt, keyForCreation()))
    override fun matches(pin: String, salt: ByteArray, expected: String): Boolean = runCatching {
        require(expected.startsWith(PREFIX))
        // Never generate a replacement key during verification. A lost key must fail closed.
        val key = existingKey() ?: return false
        MessageDigest.isEqual(digest(pin, salt, key), SecurityUtils.hexToBytes(expected.removePrefix(PREFIX)))
    }.getOrDefault(false)
}
