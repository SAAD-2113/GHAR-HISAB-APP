package com.sindantechnologies.gharhisab

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.AppLockMode
import com.sindantechnologies.gharhisab.util.security.PinVerifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Tests Activity/ViewModel lifetime with a fake verifier, not Android's biometric or Keystore hardware. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SecurityLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun pinSetupAndRecoveryCodeSurviveRecreationAndBackgrounding() {
        val releaseVerifier = CountDownLatch(1)
        val creations = AtomicInteger()
        val verifier = object : PinVerifier {
            override fun create(pin: String, salt: ByteArray): String {
                creations.incrementAndGet()
                check(releaseVerifier.await(20, TimeUnit.SECONDS))
                return "v3k:test-only"
            }
            override fun matches(pin: String, salt: ByteArray, expected: String) = pin == "1234"
        }
        lateinit var retained: ExternalActions
        lateinit var repository: UserPreferencesRepository
        try {
            compose.runOnIdle {
                retained = compose.activity.external
                repository = UserPreferencesRepository(compose.activity.applicationContext, verifier)
                runBlocking { repository.clearAppLock() }
                retained.showPinSetupDialog = true
                retained.createPin(repository, "1234")
            }
            compose.waitUntil(10_000) { idle(); creations.get() == 1 }
            compose.activityRule.scenario.recreate()
            compose.runOnIdle {
                assertSame(retained, compose.activity.external)
                assertTrue(retained.showPinSetupDialog)
                assertTrue(retained.securityBusy)
                assertNull(retained.recoveryKey)
            }
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            releaseVerifier.countDown()
            compose.waitUntil(10_000) { idle(); !retained.securityBusy }
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.runOnIdle {
                assertSame(retained, compose.activity.external)
                assertFalse(retained.showPinSetupDialog)
                assertFalse(retained.securityFailed)
                assertEquals(1, creations.get())
                assertNotNull(retained.recoveryKey)
            }
            runBlocking {
                assertEquals(AppLockMode.PIN, repository.securityState.first().mode)
                assertTrue(repository.verifyAppPin(retained.recoveryKey!!, recovery = true))
                assertEquals(AppLockMode.OFF, repository.securityState.first().mode)
            }
            // Sensitive recovery text is retained in memory only, never on a fresh session.
            assertNull(ExternalActions().recoveryKey)
        } finally {
            releaseVerifier.countDown()
        }
    }

    @Test fun rollbackContinuesOnceAcrossRecreationAndRetainsItsCompletionNotice() {
        val completion = CompletableDeferred<Unit>()
        var invocations = 0
        lateinit var retained: ExternalActions
        compose.runOnIdle {
            retained = compose.activity.external
            retained.showRollback = true
            retained.rollback { invocations++; completion.await() }
            retained.rollback { invocations++ }
            assertTrue(retained.rollbackBusy)
            assertEquals(1, invocations)
        }
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            assertSame(retained, compose.activity.external)
            assertTrue(retained.showRollback)
            assertTrue(retained.rollbackBusy)
            assertNull(retained.rollbackNotice)
            retained.rollback { invocations++ }
            assertEquals(1, invocations)
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        completion.complete(Unit)
        compose.waitUntil(10_000) { idle(); !retained.rollbackBusy }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle {
            assertSame(retained, compose.activity.external)
            assertEquals(1, invocations)
            assertFalse(retained.showRollback)
            assertEquals(R.string.backup_restored, retained.rollbackNotice)
            retained.dismissRollbackNotice()
            assertNull(retained.rollbackNotice)
        }
    }

    private fun idle() { org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle() }
}
