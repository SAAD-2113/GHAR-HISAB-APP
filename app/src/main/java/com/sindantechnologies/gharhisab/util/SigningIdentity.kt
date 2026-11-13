package com.sindantechnologies.gharhisab.util

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

/** Public certificate fingerprint for matching Android OAuth registration; never reads a private key. */
object SigningIdentity {
    @Suppress("DEPRECATION")
    fun sha1(context: Context): String? = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        info.signingInfo?.apkContentsSigners?.firstOrNull()?.let { signature ->
            MessageDigest.getInstance("SHA-1").digest(signature.toByteArray()).joinToString(":") { "%02X".format(it.toInt() and 255) }
        }
    }.getOrNull()
}
