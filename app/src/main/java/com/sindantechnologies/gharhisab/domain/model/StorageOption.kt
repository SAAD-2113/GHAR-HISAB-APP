package com.sindantechnologies.gharhisab.domain.model

/**
 * Storage options for financial data.
 *
 * @property key Persistence key for DataStore.
 * @property displayName Human-readable name.
 */
enum class StorageOption(
    val key: String,
    val displayName: String,
) {
    LOCAL(key = "local", displayName = "Local Storage"),
    GOOGLE_DRIVE(key = "google_drive", displayName = "Google Drive Backup");

    companion object {
        /** Returns the [StorageOption] matching the given [key], defaulting to [LOCAL]. */
        fun fromKey(key: String): StorageOption =
            entries.firstOrNull { it.key == key } ?: LOCAL
    }
}
