package com.sindantechnologies.gharhisab.domain.model

/**
 * Supported languages in GharHisab.
 *
 * @property code BCP-47 language code.
 * @property displayName Display name shown in the language selection screen.
 * @property nativeName Name in the native script.
 */
enum class Language(
    val code: String,
    val displayName: String,
    val nativeName: String,
) {
    ENGLISH(code = "en", displayName = "English", nativeName = "English"),
    URDU(code = "ur", displayName = "Urdu", nativeName = "اردو"),
    PUNJABI(code = "pa", displayName = "Punjabi", nativeName = "ਪੰਜਾਬੀ");

    companion object {
        /** Returns the [Language] matching the given [code], defaulting to [ENGLISH]. */
        fun fromCode(code: String): Language =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
