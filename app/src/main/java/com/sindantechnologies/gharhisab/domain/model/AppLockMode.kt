package com.sindantechnologies.gharhisab.domain.model

enum class AppLockMode(val key: String, val title: String) {
    OFF("off", "Disabled"),
    PIN("pin", "PIN Lock"),
    BIOMETRIC("biometric", "Biometric / Device Screen Lock");

    companion object {
        fun fromKey(key: String): AppLockMode =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: OFF
    }
}
