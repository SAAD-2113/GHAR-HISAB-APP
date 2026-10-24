package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testParseRupeesToPaisaValidWhole() {
        val result = CurrencyFormatter.parseRupeesToPaisa("500")
        assertEquals(50000L, result)
    }

    @Test
    fun testParseRupeesToPaisaValidDecimal() {
        val result = CurrencyFormatter.parseRupeesToPaisa("1250.50")
        assertEquals(125050L, result)
    }

    @Test
    fun testParseRupeesToPaisaSingleDecimalDigit() {
        val result = CurrencyFormatter.parseRupeesToPaisa("100.5")
        assertEquals(10050L, result)
    }

    @Test
    fun testParseRupeesToPaisaWithCommas() {
        val result = CurrencyFormatter.parseRupeesToPaisa("1,500.25")
        assertEquals(150025L, result)
    }

    @Test
    fun testParseRupeesToPaisaInvalidInput() {
        assertNull(CurrencyFormatter.parseRupeesToPaisa(""))
        assertNull(CurrencyFormatter.parseRupeesToPaisa("abc"))
        assertNull(CurrencyFormatter.parseRupeesToPaisa("10.999")) // more than 2 decimals
        assertNull(CurrencyFormatter.parseRupeesToPaisa("-50"))
    }

    @Test
    fun testFormatPaisa() {
        assertEquals("₨ 500.00", CurrencyFormatter.formatPaisa(50000L))
        assertEquals("₨ 1,250.50", CurrencyFormatter.formatPaisa(125050L))
        assertEquals("₨ 0.00", CurrencyFormatter.formatPaisa(0L))
    }

    @Test
    fun testFormatPaisaCompact() {
        assertEquals("₨ 500", CurrencyFormatter.formatPaisaCompact(50000L))
        assertEquals("₨ 1,250.50", CurrencyFormatter.formatPaisaCompact(125050L))
        assertEquals("₨ 0", CurrencyFormatter.formatPaisaCompact(0L))
    }
}
