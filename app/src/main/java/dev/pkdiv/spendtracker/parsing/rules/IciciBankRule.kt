package dev.pkdiv.spendtracker.parsing.rules

import dev.pkdiv.spendtracker.parsing.AmountParser
import dev.pkdiv.spendtracker.parsing.ParsedTransaction

class IciciBankRule : ParsingRule {
    override val id = "ICICIB"

    private val CARD = Regex("""\b(?:card|account|a/c|acct)\s+(?:xx|ending)?\s*(\d{4})""", RegexOption.IGNORE_CASE)
    private val MERCHANT_AT = Regex("""\bat\s+(.+?)(?=\s+(?:on|via|for)\b|$)""", RegexOption.IGNORE_CASE)
    private val MERCHANT_ON = Regex("""\bon\s+\d{1,2}[-/ ]\w+[-/ ]\d{2,4}\s+on\s+(.+?)(?=\s+Avl|\s*\.|$)""", RegexOption.IGNORE_CASE)

    override fun matches(sender: String, body: String): Boolean =
        sender.contains("ICICI", ignoreCase = true) &&
            Regex("""\b(debited|credited|spent)\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)

    override fun parse(sender: String, body: String, rawMessageRef: String): ParsedTransaction? {
        val amount = AmountParser.amount(body) ?: return null
        val merchant = MERCHANT_AT.find(body)?.groupValues?.get(1)?.trim()
            ?: MERCHANT_ON.find(body)?.groupValues?.get(1)?.trim()
            ?: "Unknown"
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
