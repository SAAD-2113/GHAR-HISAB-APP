package com.sindantechnologies.gharhisab.domain.model

enum class AppLockTimeout(val durationMillis: Long, val label: String) {
    IMMEDIATE(0L, "Immediately"),
    ONE_MINUTE(60_000L, "After 1 minute"),
    FIVE_MINUTES(300_000L, "After 5 minutes"),
    FIFTEEN_MINUTES(900_000L, "After 15 minutes");

    companion object {
        fun fromDuration(millis: Long): AppLockTimeout =
            entries.find { it.durationMillis == millis } ?: IMMEDIATE
    }
}
