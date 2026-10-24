package com.sindantechnologies.gharhisab.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Utility for formatting and parsing monetary amounts.
 * All amounts are strictly represented as Long in paisa (1 PKR = 100 paisa).
 * Rs. 500.00 = 50000 paisa
 *
 * Exact integer arithmetic is preserved without using Double/Float for math.
 */
object CurrencyFormatter {

    const val PAISA_PER_RUPEE = 100L

    /**
     * Converts a paisa amount to a formatted PKR string with two decimal places.
     * Example: 125050L -> "₨ 1,250.50"
     */
    fun formatPaisa(paisa: Long): String {
        val isNegative = paisa < 0
        val absPaisa = abs(paisa)
        val rupees = absPaisa / PAISA_PER_RUPEE
        val paisaRemainder = absPaisa % PAISA_PER_RUPEE
        val intFormatter = NumberFormat.getIntegerInstance(Locale("en", "PK"))
        val formattedRupees = intFormatter.format(rupees)
        val formattedPaisa = String.format(Locale.US, "%02d", paisaRemainder)
        val sign = if (isNegative) "-₨ " else "₨ "
        return "$sign$formattedRupees.$formattedPaisa"
    }

    /**
     * Converts a paisa amount to a compact formatted PKR string without decimals when zero.
     * Example: 50000L -> "₨ 500"
     */
    fun formatPaisaCompact(paisa: Long): String {
        val absPaisa = abs(paisa)
        val paisaRemainder = absPaisa % PAISA_PER_RUPEE
        return if (paisaRemainder == 0L) {
            val isNegative = paisa < 0
            val rupees = absPaisa / PAISA_PER_RUPEE
            val intFormatter = NumberFormat.getIntegerInstance(Locale("en", "PK"))
            val sign = if (isNegative) "-₨ " else "₨ "
            "$sign${intFormatter.format(rupees)}"
        } else {
            formatPaisa(paisa)
        }
    }

    /**
     * Converts a user-entered PKR string (e.g., "1250.50" or "1250") to paisa as Long.
     * Validates input characters, decimal format, and prevents overflow.
     * Returns null if the string is invalid or empty.
     */
    fun parseRupeesToPaisa(input: String): Long? {
        val cleaned = input.replace(",", "").trim()
        if (cleaned.isEmpty()) return null

        // Validate characters: only digits and at most one decimal point
        if (!cleaned.matches(Regex("""^\d+(\.\d{1,2})?$"""))) {
            return null
        }

        return try {
            val parts = cleaned.split(".")
            val rupees = parts[0].toLongOrNull() ?: return null

            // Guard against potential Long overflow when multiplying by 100
            if (rupees > Long.MAX_VALUE / PAISA_PER_RUPEE - 100) return null

            val paisa = if (parts.size > 1) {
                val paisaStr = parts[1].padEnd(2, '0')
                paisaStr.toLongOrNull() ?: return null
            } else {
                0L
            }
            rupees * PAISA_PER_RUPEE + paisa
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats paisa as a raw decimal string without currency symbol.
     * Example: 125050L -> "1,250.50"
     */
    fun formatPaisaRaw(paisa: Long): String {
        val isNegative = paisa < 0
        val absPaisa = abs(paisa)
        val rupees = absPaisa / PAISA_PER_RUPEE
        val paisaRemainder = absPaisa % PAISA_PER_RUPEE
        val intFormatter = NumberFormat.getIntegerInstance(Locale("en", "PK"))
        val formattedRupees = intFormatter.format(rupees)
        val formattedPaisa = String.format(Locale.US, "%02d", paisaRemainder)
        val sign = if (isNegative) "-" else ""
        return "$sign$formattedRupees.$formattedPaisa"
    }
}
