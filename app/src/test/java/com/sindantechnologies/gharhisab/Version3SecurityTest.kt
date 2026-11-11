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
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Version3SecurityTest {
    @Test fun defaultOffAndRecoveryDisablePinWithoutRemovingBalance() = runBlocking {
        val repo = UserPreferencesRepository(ApplicationProvider.getApplicationContext<Context>())
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
        val repo = UserPreferencesRepository(context)
        repo.setAppLockPin("1234")
        repeat(5) { assertFalse(repo.verifyAppPin("0000")) }
        val reopened = UserPreferencesRepository(context)
        assertFalse(reopened.verifyAppPin("1234"))
        ShadowSystemClock.advanceBy(Duration.ofSeconds(31))
        assertTrue(reopened.verifyAppPin("1234"))
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
