package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.util.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun testPinHashingAndVerification() {
        val pin = "1234"
        val salt = SecurityUtils.generateSalt()
        val saltHex = SecurityUtils.bytesToHex(salt)
        val hash = SecurityUtils.hashPin(pin, salt)

        // Valid pin matches
        assertTrue(SecurityUtils.verifyPin("1234", saltHex, hash))

        // Incorrect pin fails
        assertFalse(SecurityUtils.verifyPin("0000", saltHex, hash))
        assertFalse(SecurityUtils.verifyPin("1235", saltHex, hash))
    }

    @Test
    fun testAesGcmEncryptionDecryption() {
        val originalText = "GharHisab Secret Financial Backup Data 2026"
        val password = "SecureMasterPassword123!"

        val encrypted = SecurityUtils.encryptAesGcm(originalText.toByteArray(Charsets.UTF_8), password)
        val decrypted = SecurityUtils.decryptAesGcm(encrypted, password)

        assertEquals(originalText, String(decrypted, Charsets.UTF_8))
    }

    @Test(expected = Exception::class)
    fun testAesGcmDecryptionWrongPasswordThrows() {
        val originalText = "Sensitive Data"
        val password = "PasswordA"
        val wrongPassword = "PasswordB"

        val encrypted = SecurityUtils.encryptAesGcm(originalText.toByteArray(Charsets.UTF_8), password)
        SecurityUtils.decryptAesGcm(encrypted, wrongPassword)
    }
}
