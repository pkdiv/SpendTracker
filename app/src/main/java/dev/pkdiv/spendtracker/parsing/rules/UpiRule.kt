package dev.pkdiv.spendtracker.parsing.rules

import dev.pkdiv.spendtracker.parsing.AmountParser
import dev.pkdiv.spendtracker.parsing.ParsedTransaction

class UpiRule : ParsingRule {
    override val id = "UPI"

    private val MERCHANT = Regex("""(?:to|To)\s+(?:VPA\s+)?([A-Za-z0-9._-]+@[A-Za-z0-9]+|[A-Za-z0-9 .&'-]{2,40})""")

    override fun matches(sender: String, body: String): Boolean =
        (sender.contains("UPI", ignoreCase = true) || body.contains("UPI", ignoreCase = true) ||
            body.contains("@", ignoreCase = true)) &&
            Regex("""\b(debited|paid|sent|credited|received)\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)

    override fun parse(sender: String, body: String, rawMessageRef: String): ParsedTransaction? {
        val amount = AmountParser.amount(body) ?: return null
        val merchant = MERCHANT.find(body)?.groupValues?.get(1)?.trim() ?: "Unknown"
        return ParsedTransaction(
            amount = amount,
            direction = AmountParser.direction(body),
            merchant = merchant,
            account = null,
            timestamp = AmountParser.timestamp(body),
            rawMessageRef = rawMessageRef,
        )
    }
}
