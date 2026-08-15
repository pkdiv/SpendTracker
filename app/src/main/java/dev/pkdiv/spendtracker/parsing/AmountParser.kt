package dev.pkdiv.spendtracker.parsing

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AmountParser {
    private val AMOUNT = Regex("""(?:Rs\.?|INR|₹)\s*([0-9][0-9,]*\.?[0-9]*)""", RegexOption.IGNORE_CASE)
    private val CREDIT_WORDS = Regex("""\b(credited|credit|received|refund|added)\b""", RegexOption.IGNORE_CASE)

    private val DATE_PATTERNS = listOf(
        "dd-MM-yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss",
        "dd-MMM-yyyy HH:mm:ss",
        "dd MMM yyyy HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "dd-MM-yy HH:mm:ss",
        "dd/MM/yy HH:mm:ss",
    )

    private val DATE_FORMATTERS: List<DateTimeFormatter> = DATE_PATTERNS.map { pattern ->
        DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
    }

    private val DATE_TEXT = Regex("""\b\d{1,2}[-/ ]\w+[-/ ]\d{2,4}\s+\d{1,2}:\d{2}(?::\d{2})?""")

    fun amount(body: String): BigDecimal? =
        AMOUNT.find(body)?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull()

    fun direction(body: String) =
        if (CREDIT_WORDS.containsMatchIn(body)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

    fun timestamp(body: String): Instant? {
        val text = DATE_TEXT.find(body)?.value?.trim() ?: return null
        for (formatter in DATE_FORMATTERS) {
            runCatching {
                return LocalDateTime.parse(text, formatter)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            }
        }
        return null
    }
}
