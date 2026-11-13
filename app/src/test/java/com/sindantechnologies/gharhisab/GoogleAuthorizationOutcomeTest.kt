package com.sindantechnologies.gharhisab

import android.app.Activity
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.sindantechnologies.gharhisab.data.backup.GoogleAuthorizationOutcome
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleAuthorizationOutcomeTest {
    @Test fun canceledActivityPreservesPayloadAndReportsConfigurationError() {
        val state = ExternalActions()
        val data = Intent().putExtra("statusCode", 10)
        state.recordGoogleResult(Activity.RESULT_CANCELED, data)
        assertSame(data, state.googleResult!!.data)
        val outcome = GoogleAuthorizationOutcome.decode(state.googleResult!!.data) { throw ApiException(Status(it.getIntExtra("statusCode", 0))) }
        assertEquals(GoogleAuthorizationOutcome.Result.Failure(10, false), outcome)
    }
    @Test fun onlyExplicitSdkCancellationIsLabelledCancelled() {
        assertEquals(GoogleAuthorizationOutcome.Result.Failure(16, true), GoogleAuthorizationOutcome.decode(Intent()) { throw ApiException(Status(16)) })
        assertEquals(GoogleAuthorizationOutcome.Result.Failure(), GoogleAuthorizationOutcome.decode<String>(null) { error("must not run") })
    }
    @Test fun validResultReachesAuthorizationConsumer() {
        assertEquals(GoogleAuthorizationOutcome.Result.Success("authorized"), GoogleAuthorizationOutcome.decode(Intent()) { "authorized" })
    }
}
