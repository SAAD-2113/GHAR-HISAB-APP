package com.sindantechnologies.gharhisab.util

import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import java.util.Calendar

/**
 * Result of parsing natural language speech into a structured transaction proposal.
 */
data class ParsedVoiceTransaction(
    val type: TransactionType,
    val amountPaisa: Long?,
    val matchedCategory: CategoryEntity?,
    val description: String,
    val timestamp: Long,
    val originalText: String,
    val isAmbiguous: Boolean,
    val parsingNotes: String? = null
)

/**
 * Natural language parser for extracting financial transaction data from speech transcripts
 * in English, Urdu, and Punjabi.
 */
object VoiceTransactionParser {

    /**
     * Parses spoken text in English, Urdu, or Punjabi into a [ParsedVoiceTransaction].
     */
    fun parse(
        transcript: String,
        categories: List<CategoryEntity>,
        languageCode: String = "en"
    ): ParsedVoiceTransaction {
        val text = transcript.trim()
        val lowerText = text.lowercase()

        // 1. Extract Amount in Paisa
        val amountPaisa = extractAmountPaisa(text)

        // 2. Extract Transaction Type (Income vs Expense)
        val type = extractTransactionType(lowerText, text)

        // 3. Match Category
        val matchedCategory = matchCategory(lowerText, text, categories, type)

        // 4. Extract Date
        val timestamp = extractDate(lowerText, text)

        // 5. Clean Description
        val description = buildCleanDescription(text, matchedCategory?.name)

        val isAmbiguous = amountPaisa == null || amountPaisa <= 0L

        val notes = when {
            amountPaisa == null -> "Could not detect the amount. Please enter it manually."
            matchedCategory == null -> "Please select a category for this transaction."
            else -> null
        }

        return ParsedVoiceTransaction(
            type = type,
            amountPaisa = amountPaisa,
            matchedCategory = matchedCategory,
            description = description,
            timestamp = timestamp,
            originalText = text,
            isAmbiguous = isAmbiguous,
            parsingNotes = notes
        )
    }

    /**
     * Extracts monetary amount in paisa (1 PKR = 100 paisa) from spoken transcript.
     */
    fun extractAmountPaisa(text: String): Long? {
        // Step A: Replace Eastern Arabic / Urdu Numerals with Latin digits
        var normalized = text
            .replace('۰', '0').replace('۱', '1').replace('۲', '2')
            .replace('۳', '3').replace('۴', '4').replace('۵', '5')
            .replace('۶', '6').replace('۷', '7').replace('۸', '8')
            .replace('۹', '9')
            // Gurmukhi numerals
            .replace('੦', '0').replace('੧', '1').replace('੨', '2')
            .replace('੩', '3').replace('੪', '4').replace('੫', '5')
            .replace('੬', '6').replace('੭', '7').replace('੮', '8')
            .replace('੯', '9')

        // Step B: Look for explicit numbers in text (e.g., "500", "1,200", "250.50")
        val digitRegex = Regex("""\b(\d+[\d,]*(\.\d{1,2})?)\b""")
        val match = digitRegex.find(normalized)
        if (match != null) {
            val numStr = match.value.replace(",", "")
            val paisa = CurrencyFormatter.parseRupeesToPaisa(numStr)
            if (paisa != null && paisa > 0L) {
                // Check if followed by "thousand", "hazar", "lakh"
                val afterNumber = normalized.substring(match.range.last + 1).trimStart().lowercase()
                return when {
                    afterNumber.startsWith("thousand") || afterNumber.startsWith("hazar") || afterNumber.startsWith("ہزار") || afterNumber.startsWith("ਹਜ਼ਾਰ") ->
                        paisa * 1000L
                    afterNumber.startsWith("lakh") || afterNumber.startsWith("lac") || afterNumber.startsWith("لاکھ") || afterNumber.startsWith("ਲੱਖ") ->
                        paisa * 100000L
                    else -> paisa
                }
            }
        }

        // Step C: Urdu / Punjabi word number parsing
        val urduAmount = parseUrduWords(normalized)
        if (urduAmount != null && urduAmount > 0L) {
            return urduAmount * CurrencyFormatter.PAISA_PER_RUPEE
        }

        // Step D: English word number parsing
        val englishAmount = parseEnglishWords(normalized.lowercase())
        if (englishAmount != null && englishAmount > 0L) {
            return englishAmount * CurrencyFormatter.PAISA_PER_RUPEE
        }

        return null
    }

    private fun extractTransactionType(lowerText: String, originalText: String): TransactionType {
        val incomeKeywords = listOf(
            "salary", "earned", "earn", "income", "received", "got paid", "freelance", "profit",
            "تنخواہ", "آمدنی", "ملے", "ملی", "کمائی", "منافع", "تحفہ", "وصول", "انعام",
            "ਤਨਖਾਹ", "ਕਮਾਈ", "ਮਿਲੇ"
        )
        for (kw in incomeKeywords) {
            if (lowerText.contains(kw) || originalText.contains(kw)) {
                return TransactionType.INCOME
            }
        }
        return TransactionType.EXPENSE
    }

    private fun matchCategory(
        lowerText: String,
        originalText: String,
        categories: List<CategoryEntity>,
        type: TransactionType
    ): CategoryEntity? {
        val combined = "$lowerText $originalText"

        val categoryKeywords = mapOf(
            "Groceries" to listOf("groceries", "grocery", "vegetable", "vegetables", "sabzi", "sabziyan", "fruit", "fruits", "ration", "rashan", "milk", "doodh", "bread", "atta", "daal", "سبزی", "راشن", "آٹا", "دال", "چینی", "پھل", "دودھ", "سودا سلف", "ਸਬਜ਼ੀ", "ਰਾਸ਼ਨ"),
            "Utilities" to listOf("utilities", "utility", "electricity", "bijli", "gas", "water", "pani", "internet", "wifi", "bill", "bills", "بجلی", "گیس", "پانی", "بل", "انٹرنیٹ", "ਵਾਈਫਾਈ", "ਬਿਜਲੀ", "ਬਿੱਲ"),
            "Transport" to listOf("transport", "petrol", "fuel", "diesel", "car", "bike", "fare", "uber", "indrive", "careem", "rickshaw", "rikshaw", "bus", "van", "taxi", "گاڑی", "بائیک", "پیٹرول", "ایندھن", "کرایہ", "رکشہ", "ٹیکسی", "پٹرول", "ਕਿਰਾਇਆ", "ਪੈਟਰੋਲ"),
            "Education" to listOf("education", "school", "college", "university", "fees", "fee", "books", "tuition", "academy", "اسکول", "فیس", "کتابیں", "تعلیم", "کالج", "اکیڈمی", "ਟਿਊਸ਼ਨ", "ਫੀਸ", "ਸਕੂਲ"),
            "Health" to listOf("health", "medicine", "doctor", "hospital", "clinic", "pharmacy", "dawa", "dawai", "medical", "میڈیکل", "دوا", "دوائی", "ڈاکٹر", "ہسپتال", "علاج", "کلینک", "ਸਿਹਤ", "ਦਵਾਈ", "ਡਾਕਟਰ"),
            "Rent" to listOf("rent", "house rent", "room rent", "کرایہ مکان", "دکان کا کرایہ", "مکان کا کرایہ"),
            "Dining" to listOf("dining", "food", "lunch", "dinner", "breakfast", "restaurant", "hotel", "cafe", "biryani", "pizza", "burger", "chai", "tea", "khana", "کھانا", "ہوٹل", "چائے", "ناشتہ", "پیزا", "برگر", "بریانی", "ਰੈਸਟੋਰੈਂਟ", "ਖਾਣਾ", "ਚਾਹ"),
            "Shopping" to listOf("shopping", "clothes", "shoes", "dress", "mall", "cloth", "kapray", "شاپنگ", "کپڑے", "جوتے", "بازار", "مال", "ਖਰੀਦਦਾਰੀ", "ਕੱਪੜੇ"),
            "Salary" to listOf("salary", "paycheck", "monthly pay", "wage", "تنخواہ", "ماہوار", "تنخاہ", "ਤਨਖਾਹ"),
            "Business" to listOf("business", "sale", "sales", "client", "customer", "karobar", "کاروبار", "بکری", "گاہک", "دکان", "ਕਸਟਮਰ", "ਕਾਰੋਬਾਰ"),
            "Freelance" to listOf("freelance", "upwork", "fiverr", "freelancing", "project", "فری لانس", "فری لانسنگ", "پراجیکٹ", "ਫ੍ਰੀਲਾਂਸ"),
            "Gift" to listOf("gift", "eidi", "present", "تحفہ", "عیدی", "نذرانہ", "ਤੋਹਫ਼ਾ")
        )

        // First attempt: match against active categories of the same TransactionType
        for ((catName, keywords) in categoryKeywords) {
            val matchedCat = categories.find { it.name.equals(catName, ignoreCase = true) && it.transactionType == type }
            if (matchedCat != null) {
                for (kw in keywords) {
                    if (combined.contains(kw, ignoreCase = true)) {
                        return matchedCat
                    }
                }
            }
        }

        // Second attempt: match against any active category by name or keywords
        for (category in categories) {
            if (combined.contains(category.name.lowercase())) {
                return category
            }
        }

        // Fallback: return default category or "Other"
        return categories.find { it.name.equals("Other", ignoreCase = true) && it.transactionType == type }
            ?: categories.firstOrNull { it.transactionType == type }
    }

    private fun extractDate(lowerText: String, originalText: String): Long {
        val calendar = Calendar.getInstance()
        val combined = "$lowerText $originalText"
        when {
            combined.contains("yesterday") || combined.contains("گزشتہ کل") || combined.contains("پچھلے کل") -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            combined.contains("day before yesterday") || combined.contains("پرسوں") -> {
                calendar.add(Calendar.DAY_OF_YEAR, -2)
            }
            // default is today (current time)
        }
        return calendar.timeInMillis
    }

    private fun buildCleanDescription(text: String, categoryName: String?): String {
        val trimmed = text.trim()
        return if (trimmed.length > 50) {
            trimmed.take(47) + "..."
        } else if (trimmed.isNotBlank()) {
            trimmed
        } else {
            categoryName ?: "Voice Entry"
        }
    }

    private fun parseUrduWords(text: String): Long? {
        val units = mapOf(
            "ایک" to 1L, "دو" to 2L, "تین" to 3L, "چار" to 4L, "پانچ" to 5L,
            "چھ" to 6L, "سات" to 7L, "آٹھ" to 8L, "نو" to 9L, "دس" to 10L,
            "گیارہ" to 11L, "بارہ" to 12L, "تیرہ" to 13L, "چودہ" to 14L, "پندرہ" to 15L,
            "سولہ" to 16L, "سترہ" to 17L, "اٹھارہ" to 18L, "انیس" to 19L, "بیس" to 20L,
            "تیس" to 30L, "چالیس" to 40L, "پچاس" to 50L, "ساٹھ" to 60L, "ستر" to 70L,
            "اسی" to 80L, "نوے" to 90L,
            // Punjabi variations
            "ہک" to 1L, "ترے" to 3L, "پنج" to 5L, "ست" to 7L, "اٹھ" to 8L,
            "ویہہ" to 20L, "تریہہ" to 30L, "پنجاہ" to 50L, "ਸੌ" to 100L, "ਪੰਜ" to 5L
        )

        var total = 0L
        var current = 0L

        val words = text.split(Regex("""[\s,]+"""))
        var foundAny = false

        for (word in words) {
            val w = word.trim()
            val unit = units[w]
            when {
                unit != null -> {
                    current += unit
                    foundAny = true
                }
                w == "سو" || w == "سئو" || w == "ਸੌ" -> {
                    current = if (current == 0L) 100L else current * 100L
                    total += current
                    current = 0L
                    foundAny = true
                }
                w == "ہزار" || w == "ਹਜ਼ਾਰ" -> {
                    current = if (current == 0L) 1000L else current * 1000L
                    total += current
                    current = 0L
                    foundAny = true
                }
                w == "لاکھ" || w == "ਲੱਖ" -> {
                    current = if (current == 0L) 100000L else current * 100000L
                    total += current
                    current = 0L
                    foundAny = true
                }
            }
        }
        total += current
        return if (foundAny && total > 0L) total else null
    }

    private fun parseEnglishWords(text: String): Long? {
        val wordsMap = mapOf(
            "one" to 1L, "two" to 2L, "three" to 3L, "four" to 4L, "five" to 5L,
            "six" to 6L, "seven" to 7L, "eight" to 8L, "nine" to 9L, "ten" to 10L,
            "eleven" to 11L, "twelve" to 12L, "thirteen" to 13L, "fourteen" to 14L,
            "fifteen" to 15L, "sixteen" to 16L, "seventeen" to 17L, "eighteen" to 18L,
            "nineteen" to 19L, "twenty" to 20L, "thirty" to 30L, "forty" to 40L,
            "fifty" to 50L, "sixty" to 60L, "seventy" to 70L, "eighty" to 80L,
            "ninety" to 90L
        )

        var total = 0L
        var current = 0L
        var foundAny = false

        val words = text.split(Regex("""[\s,-]+"""))
        for (w in words) {
            val num = wordsMap[w]
            when {
                num != null -> {
                    current += num
                    foundAny = true
                }
                w == "hundred" -> {
                    current = if (current == 0L) 100L else current * 100L
                    foundAny = true
                }
                w == "thousand" -> {
                    current = if (current == 0L) 1000L else current * 1000L
                    total += current
                    current = 0L
                    foundAny = true
                }
                w == "lakh" || w == "lac" -> {
                    current = if (current == 0L) 100000L else current * 100000L
                    total += current
                    current = 0L
                    foundAny = true
                }
            }
        }
        total += current
        return if (foundAny && total > 0L) total else null
    }
}
