package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.util.*
import org.junit.Assert.*
import org.junit.Test

class VoiceEntryRegressionTest {
    private val shopping = CategoryEntity(1, "Shopping", TransactionType.EXPENSE, "shopping_cart")
    private val salary = CategoryEntity(2, "Salary", TransactionType.INCOME, "home")
    private val groceries = CategoryEntity(3, "Groceries", TransactionType.EXPENSE, "home")
    private val categories = listOf(shopping, salary, groceries)
    private fun parse(text: String, language: String = "en") = VoiceTransactionParser.parse(text, categories, language)

    @Test fun descriptionThenAmountPreservesDescriptionAndCategoryInEverySupportedScript() {
        for ((language, description, amount) in listOf(
            Triple("en", "shopping", "50000"),
            Triple("ur", "شاپنگ", "۵۰۰۰۰"),
            Triple("ur", "شاپنگ", "٥٠٠٠٠"),
            Triple("pa", "ਖਰੀਦਦਾਰੀ", "੫੦੦੦੦"),
            Triple("pa", "شاپنگ", "۵۰۰۰۰"),
        )) {
            val first = VoiceEntryDraft().merge(parse(description, language))
            val next = first.merge(parse(amount, language))
            assertEquals(description, next.description)
            assertEquals(shopping.id, next.category?.id)
            assertEquals(5_000_000L, CurrencyFormatter.parseRupeesToPaisa(next.amountText))
        }
    }
    @Test fun combinedUtteranceStripsOnlyItsAmountAndCurrency() {
        for ((input, expected) in listOf(
            "shopping 50000" to "shopping",
            "shopping 50,000 rupees" to "shopping",
            "شاپنگ ۵۰٬۰۰۰ روپے" to "شاپنگ",
            "ਖਰੀਦਦਾਰੀ ੫੦੦੦੦ ਰੁਪਏ" to "ਖਰੀਦਦਾਰੀ",
            "shopping fifty thousand rupees" to "shopping",
            "شاپنگ پچاس ہزار روپے" to "شاپنگ",
            "شاپنگ پنجاہ ہزار روپے" to "شاپنگ",
            "ਖਰੀਦਦਾਰੀ ਪੰਜਾਹ ਹਜ਼ਾਰ ਰੁਪਏ" to "ਖਰੀਦਦਾਰੀ",
        )) {
            val result = parse(input)
            assertEquals(input, 5_000_000L, result.amountPaisa)
            assertEquals(expected, result.description)
        }
    }
    @Test fun amountThenDescriptionAndRepeatedAmountAreIncremental() {
        val amount = VoiceEntryDraft().merge(parse("50000"))
        assertEquals("", amount.description)
        val withDescription = amount.merge(parse("shopping"))
        val corrected = withDescription.merge(parse("45000"))
        assertEquals("shopping", corrected.description)
        assertEquals(4_500_000L, CurrencyFormatter.parseRupeesToPaisa(corrected.amountText))
        assertEquals(shopping, corrected.category)
    }
    @Test fun manualEditsRemainAuthoritativeAcrossFurtherDictation() {
        val edited = VoiceEntryDraft().merge(parse("shopping 50000")).copy(
            amountText = "12.50", description = "School uniform", category = groceries,
            manualAmount = true, manualDescription = true, manualCategory = true)
        val next = edited.merge(parse("shopping 90000"))
        assertEquals("12.50", next.amountText)
        assertEquals("School uniform", next.description)
        assertEquals(groceries, next.category)
    }
    @Test fun amountOnlyDoesNotResetIncomeTypeOrDate() {
        val first = VoiceEntryDraft().merge(parse("salary yesterday"))
        val next = first.merge(parse("50000"))
        assertEquals(TransactionType.INCOME, next.type)
        assertEquals(salary, next.category)
        assertEquals(first.timestamp, next.timestamp)
        assertEquals(first.description, next.description)
    }
    @Test fun decimalPaisaAreNotTruncated() {
        for (text in listOf("shopping 125.50", "شاپنگ ۱۲۵٫۵۰", "ਖਰੀਦਦਾਰੀ ੧੨੫.੫੦")) {
            val result = VoiceEntryDraft().merge(parse(text))
            assertEquals(12_550L, CurrencyFormatter.parseRupeesToPaisa(result.amountText))
        }
    }
    @Test fun mixedNumberScalesAndHundredsRemainExact() {
        for ((input, expectedRupees) in listOf(
            "shopping 50 thousand" to 50000L,
            "shopping one lakh fifty thousand" to 150000L,
            "شاپنگ پانچ سو پچاس روپے" to 550L,
            "ਖਰੀਦਦਾਰੀ ਪੰਜ ਸੌ ਪੰਜਾਹ ਰੁਪਏ" to 550L,
            "shopping one hundred and twenty five" to 125L,
            "شاپنگ پچیس ہزار" to 25000L,
        )) assertEquals(input, expectedRupees * 100, parse(input).amountPaisa)
    }
    @Test fun ambiguousOrInvalidAmountNeverOverwritesEarlierFields() {
        val first = VoiceEntryDraft().merge(parse("shopping 500"))
        for (text in listOf("500 600", "-500", "PKR -500", "-PKR 500", "9000000000000000000 thousand", "0", "12.345")) {
            val parsed = parse(text)
            assertNull(text, parsed.amountPaisa)
            val next = first.merge(parsed)
            assertEquals(text, first.amountText, next.amountText)
            assertEquals(text, first.description, next.description)
        }
    }
    @Test fun currencyQualifiedAmountWinsOverItemQuantity() {
        val parsed = parse("2 shirts 500 rupees")
        assertEquals(50000L, parsed.amountPaisa)
        assertEquals("2 shirts", parsed.description)
    }
    @Test fun unrelatedWordsDoNotFormOneAmountAcrossTheSentence() {
        assertNull(parse("five shirts and ten shoes").amountPaisa)
    }
    @Test fun romanWordsAreInterpretedOnlyForTheSelectedLanguage() {
        assertNull(parse("do shopping", "en").amountPaisa)
        assertEquals("do shopping", parse("do shopping", "en").description)
        assertEquals(50000L, parse("shopping panch sau", "ur").amountPaisa)
        assertEquals(50000L, parse("shopping panj sau", "pa").amountPaisa)
    }
    @Test fun unicodeDescriptionPreservesItsOriginalOffsets() {
        val parsed = parse("İstanbul shopping 500")
        assertEquals("İstanbul shopping", parsed.description)
        assertEquals(50000L, parsed.amountPaisa)
    }
    @Test fun calendarDateDigitsDoNotBecomeAmountsAndRequireManualDate() {
        for (input in listOf("shopping November 13", "shopping 13 November 2026", "shopping 13/11/2026", "shopping 2026-11-13", "شاپنگ ۱۳ نومبر ۲۰۲۶", "ਖਰੀਦਦਾਰੀ ੧੩ ਨਵੰਬਰ ੨੦੨੬", "shopping 13/11")) {
            val parsed = parse(input)
            assertNull(input, parsed.amountPaisa)
            assertTrue(input, parsed.hasUnsupportedDate)
            assertTrue(input, VoiceEntryDraft().merge(parsed).requiresManualDate)
        }
        val combined = parse("shopping 50000 on 13 November 2026")
        assertEquals(5000000L, combined.amountPaisa)
        assertTrue(combined.hasUnsupportedDate)
        assertEquals("shopping on 13 November 2026", combined.description)
    }
    @Test fun dateWarningPersistsAcrossAmountOnlyButRelativeDateCanCorrectIt() {
        val first = VoiceEntryDraft().merge(parse("shopping November 13"))
        val withAmount = first.merge(parse("50000"))
        assertTrue(withAmount.requiresManualDate)
        val corrected = withAmount.merge(parse("Today"))
        assertFalse(corrected.requiresManualDate)
        assertEquals(first.description, corrected.description)
        assertEquals(withAmount.amountText, corrected.amountText)
    }
    @Test fun decimalAmountsAreNotCalendarDates() {
        val parsed = parse("shopping 12.50")
        assertEquals(1250L, parsed.amountPaisa)
        assertFalse(parsed.hasUnsupportedDate)
    }
    @Test fun dayBeforeYesterdayIsNotMistakenForYesterday() {
        val today = java.time.Instant.ofEpochMilli(parse("today").timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val parsed = parse("shopping 500 day before yesterday")
        assertEquals(today.minusDays(2), java.time.Instant.ofEpochMilli(parsed.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
    }
}
