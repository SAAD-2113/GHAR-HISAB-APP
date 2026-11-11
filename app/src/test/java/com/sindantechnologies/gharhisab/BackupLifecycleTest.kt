package com.sindantechnologies.gharhisab

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import com.sindantechnologies.gharhisab.ui.screens.backup.BackupViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun uploadSurvivesActivityRecreationAndBackgroundingWithoutRepeating() {
        val drive = FakeDriveService().apply { uploadGate = CompletableDeferred() }
        lateinit var model: BackupViewModel
        compose.runOnIdle {
            // Install the test service in the real Activity's retained ViewModelStore.
            model = ViewModelProvider(compose.activity, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    BackupViewModel(FakeBackupOperations(), { drive }) as T
            })[BackupViewModel::class.java]
            model.driveOpen = true
            model.authorized("test-token", null)
        }
        compose.waitUntil(10_000) { idle(); !model.busy }
        compose.runOnIdle { model.upload("test password") }
        compose.waitUntil(10_000) { idle(); drive.uploads == 1 }
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            assertSame(model, compose.activity.backups)
            assertTrue(model.driveOpen)
            assertTrue(model.busy)
            assertEquals(1, drive.uploads)
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        drive.uploadGate!!.complete(Unit)
        // No financial screen is visible; the retained job is still allowed to finish.
        compose.waitUntil(10_000) { idle(); !model.busy }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle {
            assertSame(model, compose.activity.backups)
            assertEquals(R.string.drive_upload_success, model.notice.message)
            assertEquals(1, drive.uploads)
            assertNull(model.error)
        }
    }
    private fun idle() { org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle() }
}
