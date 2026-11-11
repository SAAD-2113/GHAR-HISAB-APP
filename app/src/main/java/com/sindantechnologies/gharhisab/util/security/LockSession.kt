package com.sindantechnologies.gharhisab.util.security

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Retained across rotation, deliberately not saved across process death. */
class LockSession : ViewModel() {
    var locked by mutableStateOf(true)
    var backgroundAt: Long? = null
    var timeout = 0L
    fun leave(now: Long) { backgroundAt = now }
    fun enter(now: Long) {
        backgroundAt?.let { if (now - it >= timeout || now < it) locked = true }
        backgroundAt = null
    }
}
