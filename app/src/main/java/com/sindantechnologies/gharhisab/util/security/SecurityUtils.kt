package com.sindantechnologies.gharhisab.util.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enterprise-grade cryptographic utilities for GharHisab.
 * Handles PIN hashing (PBKDF2) and AES-256-GCM encryption/decryption.
 */
object SecurityUtils {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val HASH_ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure random salt.
     */
    fun generateSalt(length: Int = SALT_LENGTH_BYTES): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Derives a salted hash for the user PIN using PBKDF2WithHmacSHA256.
     */
    fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return bytesToHex(hash)
    }

    /**
     * Verifies if the candidate PIN matches the stored salt and hash.
     */
    fun verifyPin(candidatePin: String, saltHex: String, expectedHashHex: String): Boolean {
        if (saltHex.isBlank() || expectedHashHex.isBlank()) return false
        val salt = hexToBytes(saltHex)
        val candidateHash = hashPin(candidatePin, salt)
        return candidateHash.equals(expectedHashHex, ignoreCase = true)
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM with a key derived from the password.
     * Output format: [16 bytes salt] + [12 bytes IV] + [ciphertext + 16 bytes GCM tag]
     */
    fun encryptAesGcm(plainBytes: ByteArray, password: String): ByteArray {
        val salt = generateSalt(SALT_LENGTH_BYTES)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        val spec = PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherText = cipher.doFinal(plainBytes)

        // Combine: salt + iv + cipherText
        val result = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, result, 0, salt.size)
        System.arraycopy(iv, 0, result, salt.size, iv.size)
        System.arraycopy(cipherText, 0, result, salt.size + iv.size, cipherText.size)
        return result
    }

    /**
     * Decrypts ciphertext bytes using AES-256-GCM.
     */
    fun decryptAesGcm(encryptedBytes: ByteArray, password: String): ByteArray {
        val minLength = SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + 16
        require(encryptedBytes.size >= minLength) { "Invalid encrypted data size" }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        System.arraycopy(encryptedBytes, 0, salt, 0, SALT_LENGTH_BYTES)

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(encryptedBytes, SALT_LENGTH_BYTES, iv, 0, GCM_IV_LENGTH_BYTES)

        val cipherTextLength = encryptedBytes.size - SALT_LENGTH_BYTES - GCM_IV_LENGTH_BYTES
        val cipherText = ByteArray(cipherTextLength)
        System.arraycopy(encryptedBytes, SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES, cipherText, 0, cipherTextLength)

        val spec = PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(cipherText)
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i ushr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
