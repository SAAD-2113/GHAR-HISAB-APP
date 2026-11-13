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
    val parsingNotes: String? = null,
    val hasExplicitType: Boolean = false,
    val hasExplicitDate: Boolean = false,
    val hasUnsupportedDate: Boolean = false,
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
        val amount = amountMatch(text, languageCode)
        val amountPaisa = amount?.paisa

        // 2. Extract Transaction Type (Income vs Expense)
        val type = extractTransactionType(lowerText, text)

        // 3. Match Category
        val matchedCategory = matchCategory(lowerText, text, categories, type)

        // 4. Extract Date
        val timestamp = extractDate(text)
        val unsupportedDate = absoluteDatePattern.containsMatchIn(normalize(text))

        // 5. Clean Description
        val description = buildCleanDescription(text, amount, languageCode)

        val isAmbiguous = amountPaisa == null || amountPaisa <= 0L || unsupportedDate

        val notes = when {
            unsupportedDate -> "Choose the calendar date in manual entry before saving."
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
            parsingNotes = notes,
            hasExplicitType = type == TransactionType.INCOME || matchedCategory != null ||
                listOf("spent", "expense", "paid", "خرچ", "ادا", "ਖਰਚ").any { lowerText.contains(it) },
            hasExplicitDate = relativeDatePattern.containsMatchIn(normalize(text)),
            hasUnsupportedDate = unsupportedDate,
        )
    }

    private val monthNames = "january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec|جنوری|فروری|مارچ|اپریل|مئی|جون|جولائی|اگست|ستمبر|اکتوبر|نومبر|دسمبر|ਜਨਵਰੀ|ਫਰਵਰੀ|ਮਾਰਚ|ਅਪ੍ਰੈਲ|ਮਈ|ਜੂਨ|ਜੁਲਾਈ|ਅਗਸਤ|ਸਤੰਬਰ|ਅਕਤੂਬਰ|ਨਵੰਬਰ|ਦਸੰਬਰ"
    // Absolute dates require the date picker; their digits must never become an amount.
    private val absoluteDatePattern = Regex("""(?<![\p{L}\p{N}])(?:[0-9]{1,4}[/.-][0-9]{1,2}[/.-][0-9]{1,4}|[0-9]{1,2}/[0-9]{1,2}|[0-9]{1,2}(?:st|nd|rd|th)?\s+(?:$monthNames)(?:\s+[0-9]{4})?|(?:$monthNames)\s+[0-9]{1,2}(?:st|nd|rd|th)?(?:,?\s+[0-9]{4})?)(?![\p{L}\p{N}])""")
    private val relativeDatePattern = Regex("""(?<![\p{L}\p{N}])(?:day before yesterday|yesterday|today|گزشتہ کل|پچھلے کل|پرسوں|آج|اج|ਅੱਜ)(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)

    /** A single, bounded monetary phrase. Its span is also removed from the description. */
    private data class AmountMatch(val paisa: Long, val range: IntRange, val explicitCurrency: Boolean)
    private data class Token(val text: String, val range: IntRange)
    private val currencies = setOf("rupee", "rupees", "pkr", "rs", "روپے", "روپیہ", "روپئے", "ਰੁਪਏ", "ਰੁਪਿਆ")
    private val conjunctions = setOf("and", "اور", "تے", "ਅਤੇ")
    private val scales = mapOf(
        "hundred" to 100L, "سو" to 100L, "سئو" to 100L, "ਸੌ" to 100L, "sau" to 100L,
        "thousand" to 1000L, "hazar" to 1000L, "hazaar" to 1000L, "ہزار" to 1000L, "ਹਜ਼ਾਰ" to 1000L, "ਹਜਾਰ" to 1000L,
        "lakh" to 100000L, "lac" to 100000L, "لاکھ" to 100000L, "ਲੱਖ" to 100000L,
        "million" to 1000000L, "crore" to 10000000L, "کروڑ" to 10000000L, "ਕਰੋੜ" to 10000000L,
    )
    private val numbers = buildMap<String, Long> {
        fun putSeries(words: String, start: Int = 0) = words.split(' ').forEachIndexed { i, word -> put(word, (start + i).toLong()) }
        putSeries("zero one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen")
        listOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety").forEachIndexed { i, word -> put(word, (i + 2) * 10L) }
        putSeries("صفر ایک دو تین چار پانچ چھ سات آٹھ نو دس گیارہ بارہ تیرہ چودہ پندرہ سولہ سترہ اٹھارہ انیس بیس")
        putSeries("اکیس بائیس تئیس چوبیس پچیس چھبیس ستائیس اٹھائیس انتیس تیس", 21)
        putSeries("اکتیس بتیس تینتیس چونتیس پینتیس چھتیس سینتیس اڑتیس انتالیس چالیس", 31)
        putSeries("اکتالیس بیالیس تینتالیس چوالیس پینتالیس چھیالیس سینتالیس اڑتالیس انچاس پچاس", 41)
        putSeries("اکیاون باون ترپن چوّن پچپن چھپن ستاون اٹھاون انسٹھ ساٹھ", 51)
        putSeries("اکسٹھ باسٹھ تریسٹھ چونسٹھ پینسٹھ چھیاسٹھ سڑسٹھ اڑسٹھ انہتر ستر", 61)
        putSeries("اکہتر بہتر تہتر چوہتر پچھتر چھہتر ستتر اٹھہتر اناسی اسی", 71)
        putSeries("اکیاسی بیاسی تراسی چوراسی پچاسی چھیاسی ستاسی اٹھاسی نواسی نوے", 81)
        putSeries("اکانوے بانوے ترانوے چورانوے پچانوے چھیانوے ستانوے اٹھانوے ننانوے", 91)
        putSeries("ਸਿਫਰ ਇੱਕ ਦੋ ਤਿੰਨ ਚਾਰ ਪੰਜ ਛੇ ਸੱਤ ਅੱਠ ਨੌਂ ਦਸ ਗਿਆਰਾਂ ਬਾਰਾਂ ਤੇਰਾਂ ਚੌਦਾਂ ਪੰਦਰਾਂ ਸੋਲਾਂ ਸਤਾਰਾਂ ਅਠਾਰਾਂ ਉੱਨੀ ਵੀਹ")
        listOf("ਤੀਹ", "ਚਾਲੀ", "ਪੰਜਾਹ", "ਸੱਠ", "ਸੱਤਰ", "ਅੱਸੀ", "ਨੱਬੇ").forEachIndexed { i, word -> put(word, (i + 3) * 10L) }
        putAll(mapOf("ہک" to 1L, "اک" to 1L, "ترے" to 3L, "پنج" to 5L, "چھے" to 6L, "ست" to 7L, "اٹھ" to 8L,
            "ویہہ" to 20L, "تریہہ" to 30L, "پنجاہ" to 50L, "ਪੰਜਾਹ" to 50L))
    }
    private val romanNumbers = mapOf(
            "ik" to 1L, "do" to 2L, "teen" to 3L, "tin" to 3L, "char" to 4L, "chaar" to 4L,
            "panch" to 5L, "paanch" to 5L, "panj" to 5L, "das" to 10L, "bees" to 20L, "veeh" to 20L,
            "tees" to 30L, "chalis" to 40L, "pachas" to 50L, "panjah" to 50L, "saath" to 60L,
            "sattar" to 70L, "assi" to 80L, "nabbe" to 90L)
    private fun spokenNumbers(language: String): Map<String, Long> =
        if (language.substringBefore('-') in setOf("ur", "pa")) numbers + romanNumbers else numbers
    private fun normalize(text: String): String = text.map { c ->
        when {
            Character.digit(c, 10) >= 0 -> ('0'.code + Character.digit(c, 10)).toChar()
            c == '٫' -> '.'
            c == '٬' -> ','
            c == 'ي' || c == 'ى' -> 'ی'
            c == 'ك' -> 'ک'
            else -> c.lowercaseChar()
        }
    }.joinToString("")

    fun extractAmountPaisa(text: String): Long? = amountMatch(text)?.paisa

    private fun amountMatch(text: String, languageCode: String = "en"): AmountMatch? {
        val spokenNumbers = spokenNumbers(languageCode)
        val normalized = normalize(text)
        val withoutDates = absoluteDatePattern.replace(normalized) { " ".repeat(it.value.length) }
        val tokens = Regex("""[0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?|[\p{L}\p{M}]+""")
            .findAll(withoutDates).map { Token(it.value, it.range) }.toList()
        fun value(token: String): java.math.BigDecimal? = spokenNumbers[token]?.toBigDecimal()
            ?: token.takeIf { it.firstOrNull()?.isDigit() == true }?.replace(",", "")?.toBigDecimalOrNull()
        val found = mutableListOf<AmountMatch>()
        var i = 0
        while (i < tokens.size) {
            if (value(tokens[i].text) == null && tokens[i].text !in scales) { i++; continue }
            val first = i
            var total = java.math.BigDecimal.ZERO
            var current = java.math.BigDecimal.ZERO
            var previousDigit = false
            var previousScale = false
            while (i < tokens.size) {
                val word = tokens[i].text
                val number = value(word)
                val scale = scales[word]
                if (word in conjunctions && i + 1 < tokens.size && value(tokens[i + 1].text) != null) { i++; continue }
                if (number != null) {
                    // Separate literal amounts are ambiguous; do not silently add them together.
                    if (i > first && previousDigit && !previousScale && word.first().isDigit()) break
                    current += number
                    previousDigit = word.first().isDigit()
                    previousScale = false
                } else if (scale != null) {
                    if (previousScale && current.signum() == 0) break
                    current = (if (current.signum() == 0) java.math.BigDecimal.ONE else current) * scale.toBigDecimal()
                    if (scale >= 1000) { total += current; current = java.math.BigDecimal.ZERO }
                    previousScale = true
                    previousDigit = false
                } else break
                i++
            }
            var from = tokens[first].range.first
            var to = tokens[i - 1].range.last
            var currency = false
            if (first > 0 && tokens[first - 1].text in currencies) { from = tokens[first - 1].range.first; currency = true }
            if (i < tokens.size && tokens[i].text in currencies) { to = tokens[i].range.last; i++; currency = true }
            if (from > 0 && normalized.substring(0, from).trimEnd().endsWith("₨")) {
                from = normalized.lastIndexOf('₨', from - 1); currency = true
            }
            // Reject negatives and overly precise/overflowing input; never truncate money.
            val numberStart = tokens[first].range.first
            if ((from > 0 && normalized.substring(0, from).trimEnd().endsWith('-')) ||
                (numberStart > 0 && normalized.substring(0, numberStart).trimEnd().endsWith('-'))) continue
            val paisa = runCatching { ((total + current) * 100.toBigDecimal()).longValueExact() }.getOrNull()
            if (paisa != null && paisa > 0) found.add(AmountMatch(paisa, from..to, currency))
        }
        val explicit = found.filter { it.explicitCurrency }
        return when {
            explicit.size == 1 -> explicit.single()
            found.size == 1 -> found.single()
            else -> null
        }
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
        for (category in categories.filter { it.transactionType == type }) {
            if (combined.contains(category.name.lowercase())) {
                return category
            }
        }

        // No match is different from explicitly speaking "Other". Do not overwrite an earlier selection.
        return null
    }

    private fun extractDate(text: String): Long {
        val calendar = Calendar.getInstance()
        val relativeDate = relativeDatePattern.find(normalize(text))?.value
        when (relativeDate) {
            "day before yesterday", "پرسوں" -> calendar.add(Calendar.DAY_OF_YEAR, -2)
            "yesterday", "گزشتہ کل", "پچھلے کل" -> calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return calendar.timeInMillis
    }

    private fun buildCleanDescription(text: String, amount: AmountMatch?, languageCode: String): String {
        val spokenNumbers = spokenNumbers(languageCode)
        val withoutAmount = if (amount == null) text else text.removeRange(amount.range)
        val withoutRelativeDates = relativeDatePattern.replace(withoutAmount, "")
        val cleaned = withoutRelativeDates.replace(Regex("""\s+"""), " ").trim(' ', ',', '.', ':', ';', '-', '،')
        val words = Regex("""[\p{L}\p{M}]+""").findAll(normalize(cleaned)).map { it.value }.toList()
        // Invalid/ambiguous numeric-only speech must not replace an existing description either.
        return if (words.all { it in spokenNumbers || it in scales || it in currencies || it in conjunctions }) "" else cleaned
    }
}

/** Only fields present in the new utterance are merged. Manual edits remain authoritative. */
data class VoiceEntryDraft(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val category: CategoryEntity? = null,
    val description: String = "",
    val timestamp: Long? = null,
    val manualType: Boolean = false,
    val manualAmount: Boolean = false,
    val manualCategory: Boolean = false,
    val manualDescription: Boolean = false,
    val requiresManualDate: Boolean = false,
) {
    fun merge(parsed: ParsedVoiceTransaction): VoiceEntryDraft {
        val nextType = if (!manualType && parsed.hasExplicitType) parsed.type else type
        val incomingCategory = parsed.matchedCategory?.takeIf { it.transactionType == nextType }
        val retainedCategory = category?.takeIf { it.transactionType == nextType }
        return copy(
            type = nextType,
            amountText = if (!manualAmount && parsed.amountPaisa != null) CurrencyFormatter.formatPaisaRaw(parsed.amountPaisa) else amountText,
            category = if (manualCategory) retainedCategory else incomingCategory ?: retainedCategory,
            description = if (!manualDescription && parsed.description.isNotBlank()) parsed.description else description,
            timestamp = if (parsed.hasExplicitDate || timestamp == null) parsed.timestamp else timestamp,
            requiresManualDate = when {
                parsed.hasUnsupportedDate -> true
                parsed.hasExplicitDate -> false
                else -> requiresManualDate
            },
        )
    }
}
