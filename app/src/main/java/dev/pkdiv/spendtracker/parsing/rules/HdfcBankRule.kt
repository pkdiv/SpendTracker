package dev.pkdiv.spendtracker.parsing.rules

import dev.pkdiv.spendtracker.parsing.AmountParser
import dev.pkdiv.spendtracker.parsing.ParsedTransaction

class HdfcBankRule : ParsingRule {
    override val id = "HDFC-BANK"

    private val CARD = Regex("""(?:card|account|a/c|acct|Card|Account)\s+(?:xx|XX|ending|Ending)?\s*(\d{4})""")
    private val MERCHANT = Regex("""(?:at|At|AT)\s+(.+?)(?=\s+(?:on|via|for)\b|$)""")

    override fun matches(sender: String, body: String): Boolean =
        sender.contains("HDFC", ignoreCase = true) &&
            Regex("""\b(debited|credited|spent)\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)

    override fun parse(sender: String, body: String, rawMessageRef: String): ParsedTransaction? {
        val amount = AmountParser.amount(body) ?: return null
        val merchant = MERCHANT.find(body)?.groupValues?.get(1)?.trim() ?: "Unknown"
        val account = CARD.find(body)?.groupValues?.get(1)
        return ParsedTransaction(
            amount = amount,
            direction = AmountParser.direction(body),
            merchant = merchant,
            account = account,
            timestamp = AmountParser.timestamp(body),
            rawMessageRef = rawMessageRef,
        )
    }
}
