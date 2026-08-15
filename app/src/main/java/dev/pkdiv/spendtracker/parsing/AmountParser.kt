package dev.pkdiv.spendtracker.parsing

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AmountParser {
    private val CURRENCY_AMOUNT = Regex(
        """(?:Rs\.?|INR|₹|USD)\s*([0-9][0-9,]*\.?[0-9]*)""",
        RegexOption.IGNORE_CASE,
    )
    private val SPEND_AMOUNT = Regex(
        """(?:Rs\.?|INR|₹|USD)\s*([0-9][0-9,]*\.?[0-9]*)\s*(?:spent|debited|paid|charged|withdrawn)""",
        RegexOption.IGNORE_CASE,
    )
    private val SPEND_AMOUNT_PREFIX = Regex(
        """\b(?:spent|debited|paid|charged|withdrawn)\s+(?:Rs\.?|INR|₹|USD)\s*([0-9][0-9,]*\.?[0-9]*)""",
        RegexOption.IGNORE_CASE,
    )
    private val AVAILABLE_BALANCE_MARKER = Regex(
        """\bAvl(?:\s+(?:Limit|Bal|Balance))?\b""",
        RegexOption.IGNORE_CASE,
    )
    private val CREDIT_WORDS = Regex("""\b(credited|credit|received|refund|added)\b""", RegexOption.IGNORE_CASE)

    private val TIME_PATTERNS = listOf(
        "dd-MM-yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss",
        "dd-MMM-yyyy HH:mm:ss",
        "dd MMM yyyy HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "dd-MM-yy HH:mm:ss",
        "dd/MM/yy HH:mm:ss",
        "dd-MMM-yy HH:mm:ss",
    )

    private val TIME_FORMATTERS: List<DateTimeFormatter> = TIME_PATTERNS.map { pattern ->
        DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
    }

    private val DATE_PATTERNS = listOf(
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "dd-MMM-yyyy",
        "dd MMM yyyy",
        "yyyy-MM-dd",
        "dd-MM-yy",
        "dd/MM/yy",
        "dd-MMM-yy",
    )

    private val DATE_FORMATTERS: List<DateTimeFormatter> = DATE_PATTERNS.map { pattern ->
        DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
    }

    private val DATE_TEXT = Regex("""\b\d{1,2}[-/ ]\w+[-/ ]\d{2,4}(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?""")

    fun amount(body: String): BigDecimal? {
        SPEND_AMOUNT.find(body)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        SPEND_AMOUNT_PREFIX.find(body)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        val beforeAvailableBalance = body.split(AVAILABLE_BALANCE_MARKER).first()
        CURRENCY_AMOUNT.find(beforeAvailableBalance)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        return CURRENCY_AMOUNT.find(body)?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull()
    }

    fun direction(body: String) =
        if (CREDIT_WORDS.containsMatchIn(body)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

    fun timestamp(body: String): Instant? {
        val text = DATE_TEXT.find(body)?.value?.trim() ?: return null
        for (formatter in TIME_FORMATTERS) {
            runCatching {
                return LocalDateTime.parse(text, formatter)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            }
        }
        for (formatter in DATE_FORMATTERS) {
            runCatching {
                return LocalDate.parse(text, formatter)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
            }
        }
        return null
    }
}
