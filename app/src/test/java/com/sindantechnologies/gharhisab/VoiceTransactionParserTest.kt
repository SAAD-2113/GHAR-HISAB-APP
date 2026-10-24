package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.util.VoiceTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class VoiceTransactionParserTest {

    private lateinit var sampleCategories: List<CategoryEntity>

    @Before
    fun setup() {
        sampleCategories = listOf(
            CategoryEntity(id = 1L, name = "Groceries", transactionType = TransactionType.EXPENSE, iconKey = "shopping_cart"),
            CategoryEntity(id = 2L, name = "Utilities", transactionType = TransactionType.EXPENSE, iconKey = "bolt"),
            CategoryEntity(id = 3L, name = "Transport", transactionType = TransactionType.EXPENSE, iconKey = "directions_car"),
            CategoryEntity(id = 4L, name = "Salary", transactionType = TransactionType.INCOME, iconKey = "payments"),
            CategoryEntity(id = 5L, name = "Other", transactionType = TransactionType.EXPENSE, iconKey = "more_horiz")
        )
    }

    @Test
    fun testEnglishExpensePhrase() {
        val input = "I spent 500 rupees on groceries today"
        val parsed = VoiceTransactionParser.parse(input, sampleCategories, "en")

        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(50000L, parsed.amountPaisa) // Rs. 500 = 50,000 paisa
        assertEquals(1L, parsed.matchedCategory?.id)
        assertFalse(parsed.isAmbiguous)
    }

    @Test
    fun testEnglishIncomePhrase() {
        val input = "Received 45000 salary from office"
        val parsed = VoiceTransactionParser.parse(input, sampleCategories, "en")

        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(4500000L, parsed.amountPaisa) // Rs. 45,000 = 4,500,000 paisa
        assertEquals(4L, parsed.matchedCategory?.id)
        assertFalse(parsed.isAmbiguous)
    }

    @Test
    fun testUrduExpensePhrase() {
        val input = "آج سبزی پر پانچ سو روپے خرچ ہوئے"
        val parsed = VoiceTransactionParser.parse(input, sampleCategories, "ur")

        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(50000L, parsed.amountPaisa) // 500 = 50,000 paisa
        assertEquals(1L, parsed.matchedCategory?.id)
        assertFalse(parsed.isAmbiguous)
    }

    @Test
    fun testPunjabiExpensePhrase() {
        val input = "اج سبزی تے پنج سو روپے خرچ ہوئے"
        val parsed = VoiceTransactionParser.parse(input, sampleCategories, "pa")

        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(50000L, parsed.amountPaisa)
        assertEquals(1L, parsed.matchedCategory?.id)
        assertFalse(parsed.isAmbiguous)
    }

    @Test
    fun testAmbiguousAmountPhrase() {
        val input = "Bought some groceries"
        val parsed = VoiceTransactionParser.parse(input, sampleCategories, "en")

        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(null, parsed.amountPaisa)
        assertEquals(true, parsed.isAmbiguous)
        assertNotNull(parsed.parsingNotes)
    }
}
