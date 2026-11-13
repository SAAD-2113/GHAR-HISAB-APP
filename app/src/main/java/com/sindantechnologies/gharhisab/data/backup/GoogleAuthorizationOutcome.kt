package com.sindantechnologies.gharhisab.data.backup

import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

/** A canceled Activity can still return a developer/configuration error: decode its payload first. */
object GoogleAuthorizationOutcome {
    sealed interface Result<out T> {
        data class Success<T>(val value: T) : Result<T>
        data class Failure(val code: Int? = null, val userCancelled: Boolean = false) : Result<Nothing>
    }
    fun <T> decode(data: Intent?, parser: (Intent) -> T): Result<T> {
        if (data == null) return Result.Failure() // Unknown interruption is not proof the user cancelled.
        return try { Result.Success(parser(data)) }
        catch (e: ApiException) { Result.Failure(e.statusCode, e.statusCode == CommonStatusCodes.CANCELED) }
        catch (_: Exception) { Result.Failure() }
    }
}
