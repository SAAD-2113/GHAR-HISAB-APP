package com.sindantechnologies.gharhisab.domain.model

/**
 * Theme modes available in GharHisab.
 *
 * @property key Persistence key for DataStore.
 * @property displayName Human-readable name.
 */
enum class ThemeMode(
    val key: String,
    val displayName: String,
) {
    CLASSIC(key = "classic", displayName = "Classic"),
    MIDNIGHT(key = "midnight", displayName = "Midnight"),
    FOLLOW_SYSTEM(key = "follow_system", displayName = "Follow System");

    companion object {
        /** Returns the [ThemeMode] matching the given [key], defaulting to [FOLLOW_SYSTEM]. */
        fun fromKey(key: String): ThemeMode =
            entries.firstOrNull { it.key == key } ?: FOLLOW_SYSTEM
    }
}
