package com.sindantechnologies.gharhisab

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.*

/** Pending external results survive relocking/rotation; credentials never enter saved-state bundles. */
class ExternalActions : ViewModel() {
    var googleResult by mutableStateOf<Intent?>(null)
    var googleCancelled by mutableStateOf(false)
    var importUri by mutableStateOf<Uri?>(null)
    var exportUri by mutableStateOf<Uri?>(null)
    var exportPassword by mutableStateOf("")
}
