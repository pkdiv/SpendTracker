package dev.pkdiv.spendtracker.ingestion.sms

/**
 * Pure pre-filter applied before parsing/storage. Unrelated personal messages must
 * never be copied into the app database, so a message is only considered
 * transaction-shaped when it carries both an amount and a transaction signal and is
 * not an OTP/verification message.
 */
object SmsFilter {

    private val AMOUNT = Regex(
        """(?i)(?:\b(?:rs|inr)\.?\s*|₹\s*|\b(?:amount|amt)\s*[:=]?\s*)[\d,]+(?:\.\d{1,2})?""",
    )

    private val TRANSACTION = Regex(
        """(?i)\b(debited|credited|spent|paid|charged|withdrawn|withdrew|deposited|""" +
            """transferred|sent|received|refunded|reversed|purchase|payment|txn|transaction)\b""",
    )

    private val OTP = Regex("""(?i)\b(otp|one[- ]time password|verification code)\b""")

    fun isTransactionShaped(sender: String, body: String): Boolean {
        if (body.isBlank()) return false
        if (OTP.containsMatchIn(body)) return false
        if (!AMOUNT.containsMatchIn(body)) return false
        return TRANSACTION.containsMatchIn(body)
    }
}
