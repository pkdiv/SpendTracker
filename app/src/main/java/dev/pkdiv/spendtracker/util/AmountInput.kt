package dev.pkdiv.spendtracker.util

import java.math.BigDecimal
import java.math.RoundingMode

/** Pure helpers for entering money amounts into text fields. */
object AmountInput {

    /** Keeps only digits and a single decimal point, so the value parses reliably. */
    fun sanitize(raw: String): String {
        var seenDot = false
        return buildString {
            raw.forEach { char ->
                when {
                    char.isDigit() -> append(char)
                    char == '.' && !seenDot -> {
                        append(char)
                        seenDot = true
                    }
                }
            }
        }
    }

    /**
     * Parses a positive amount to paise precision, or null when blank, zero,
     * negative or otherwise invalid.
     */
    fun parse(raw: String): BigDecimal? =
        raw.trim()
            .removePrefix("₹")
            .replace(",", "")
            .trim()
            .toBigDecimalOrNull()
            ?.takeIf { it.signum() > 0 }
            ?.setScale(2, RoundingMode.HALF_UP)
}
