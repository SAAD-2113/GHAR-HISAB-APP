package com.sindantechnologies.gharhisab

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.AppLockMode
import com.sindantechnologies.gharhisab.util.security.LockSession
import com.sindantechnologies.gharhisab.util.security.SecurityUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Version3SecurityTest {
    private class MemoryPinVerifier : com.sindantechnologies.gharhisab.util.security.PinVerifier {
        var available = true
        var creations = 0
        var checks = 0
        private val key = javax.crypto.spec.SecretKeySpec(ByteArray(32) { it.toByte() }, "HmacSHA256")
        private fun digest(pin: String, salt: ByteArray): ByteArray = javax.crypto.Mac.getInstance("HmacSHA256").run {
            init(key); update(salt); doFinal(pin.toByteArray())
        }
        override fun create(pin: String, salt: ByteArray): String {
            check(available); creations++
            return "v3k:" + SecurityUtils.bytesToHex(digest(pin, salt))
        }
        override fun matches(pin: String, salt: ByteArray, expected: String): Boolean {
            checks++
            return available && java.security.MessageDigest.isEqual(digest(pin, salt), SecurityUtils.hexToBytes(expected.removePrefix("v3k:")))
        }
    }
    private val verifier = MemoryPinVerifier()

    @Before fun resetSecurityPreferences() = runBlocking {
        // The top-level DataStore delegate survives multiple methods in a Robolectric sandbox.
        UserPreferencesRepository(ApplicationProvider.getApplicationContext<Context>(), verifier).clearAppLock()
    }

    @Test fun defaultOffAndRecoveryDisablePinWithoutRemovingBalance() = runBlocking {
        val repo = UserPreferencesRepository(ApplicationProvider.getApplicationContext<Context>(), verifier)
        assertEquals(AppLockMode.OFF, repo.securityState.first().mode)
        repo.setStartingBalance(123456)
        val recovery = repo.setAppLockPin("1234")
        assertTrue(repo.verifyAppPin("1234"))
        assertTrue(repo.verifyAppPin(recovery, recovery = true))
        assertEquals(AppLockMode.OFF, repo.securityState.first().mode)
        assertEquals(123456L, repo.startingBalanceFlow.first())
    }
    @Test fun throttleSurvivesRepositoryRecreationAndBlocksCorrectPinUntilDelay() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UserPreferencesRepository(context, verifier)
        repo.setAppLockPin("1234")
        repeat(5) { assertFalse(repo.verifyAppPin("0000")) }
        val reopened = UserPreferencesRepository(context, verifier)
        assertFalse(reopened.verifyAppPin("1234"))
        ShadowSystemClock.advanceBy(Duration.ofSeconds(31))
        assertTrue(reopened.verifyAppPin("1234"))
    }
    @Test fun newPinUsesDeviceBoundVerifierForRepeatedUnlocks() = runBlocking {
        val repo = UserPreferencesRepository(ApplicationProvider.getApplicationContext<Context>(), verifier)
        repo.setAppLockPin("1234")
        assertTrue(repo.appLockPinHashFlow.first().startsWith("v3k:"))
        repeat(10) { assertTrue(repo.verifyAppPin("1234")) }
        assertEquals(1, verifier.creations)
        assertEquals(10, verifier.checks)
    }
    @Test fun previousPbkdfPinUpgradesAfterFirstSuccessfulUnlock() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacy = object : com.sindantechnologies.gharhisab.util.security.PinVerifier {
            override fun create(pin: String, salt: ByteArray) = SecurityUtils.hashPin(pin, salt)
            override fun matches(pin: String, salt: ByteArray, expected: String) = error("Legacy path does not use Keystore")
        }
        UserPreferencesRepository(context, legacy).setAppLockPin("4321")
        val repo = UserPreferencesRepository(context, verifier)
        assertTrue(repo.appLockPinHashFlow.first().startsWith("v2:"))
        assertTrue(repo.verifyAppPin("4321"))
        assertTrue(repo.appLockPinHashFlow.first().startsWith("v3k:"))
        assertEquals(1, verifier.creations)
        assertTrue(repo.verifyAppPin("4321"))
        assertEquals(1, verifier.checks)
    }
    @Test fun lostDeviceKeyFailsClosedButRecoveryStillPreservesRecords() = runBlocking {
        val repo = UserPreferencesRepository(ApplicationProvider.getApplicationContext<Context>(), verifier)
        repo.setStartingBalance(90000)
        val recovery = repo.setAppLockPin("1234")
        verifier.available = false
        assertFalse(repo.verifyAppPin("1234"))
        assertTrue(repo.verifyAppPin(recovery.lowercase().chunked(4).joinToString("-"), recovery = true))
        assertEquals(AppLockMode.OFF, repo.securityState.first().mode)
        assertEquals(90000L, repo.startingBalanceFlow.first())
    }
    @Test fun failedKeyCreationDoesNotEnablePinOrChangeBalance() = runBlocking {
        val repo = UserPreferencesRepository(ApplicationProvider.getApplicationContext<Context>(), verifier)
        repo.setStartingBalance(90000)
        verifier.available = false
        assertTrue(runCatching { repo.setAppLockPin("1234") }.isFailure)
        assertEquals(AppLockMode.OFF, repo.securityState.first().mode)
        assertEquals(90000L, repo.startingBalanceFlow.first())
    }
    @Test fun recoveryCodeIsSeparateFromPinAndOldCodeExpiresAfterPinChange() = runBlocking {
        val repo = UserPreferencesRepository(ApplicationProvider.getApplicationContext<Context>(), verifier)
        val old = repo.setAppLockPin("1234")
        val current = repo.setAppLockPin("4321")
        assertFalse(repo.verifyAppPin(current))
        assertFalse(repo.verifyAppPin("4321", recovery = true))
        assertFalse(repo.verifyAppPin(old, recovery = true))
        assertTrue(repo.verifyAppPin(current, recovery = true))
    }
    @Test @Config(sdk = [29]) fun devicePinOnlyIsAvailableOnAndroid10() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.robolectric.Shadows.shadowOf(context.getSystemService(android.app.KeyguardManager::class.java)).setIsDeviceSecure(true)
        assertTrue(com.sindantechnologies.gharhisab.util.security.BiometricAuthManager.canAuthenticate(context))
        assertEquals(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            com.sindantechnologies.gharhisab.util.security.BiometricAuthManager.AUTHENTICATORS)
    }
    @Test fun UrduAndArabicPinDigitsHaveTheSameMeaningAsAscii() = runBlocking {
        val repo = UserPreferencesRepository(ApplicationProvider.getApplicationContext<Context>(), verifier)
        repo.setAppLockPin("۱۲۳۴")
        assertTrue(repo.verifyAppPin("1234"))
        assertTrue(repo.verifyAppPin("١٢٣٤"))
        assertFalse(repo.verifyAppPin("1x2x3x4"))
    }
    @Test fun legacyPinHashStillAuthenticates() {
        val salt = ByteArray(16) { it.toByte() }
        val hash = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(javax.crypto.spec.PBEKeySpec("4321".toCharArray(), salt, 10000, 256)).encoded
        assertTrue(SecurityUtils.verifyPin("4321", SecurityUtils.bytesToHex(salt), SecurityUtils.bytesToHex(hash)))
        assertFalse(SecurityUtils.verifyPin("4321", "bad salt", "not a hash"))
    }
    @Test fun relockHonorsBackgroundTimeoutAndColdStart() {
        val session = LockSession()
        assertTrue(session.locked)
        session.locked = false; session.timeout = 60000
        session.leave(1000); session.enter(2000); assertFalse(session.locked)
        session.leave(2000); session.enter(62000); assertTrue(session.locked)
        session.locked = false; session.timeout = 0
        session.leave(62000); session.enter(62000); assertTrue(session.locked)
        assertTrue(LockSession().locked)
    }
}
