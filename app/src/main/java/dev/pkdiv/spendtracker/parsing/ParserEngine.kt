package dev.pkdiv.spendtracker.parsing

import dev.pkdiv.spendtracker.parsing.rules.HdfcBankRule
import dev.pkdiv.spendtracker.parsing.rules.IciciBankRule
import dev.pkdiv.spendtracker.parsing.rules.ParsingRule
import dev.pkdiv.spendtracker.parsing.rules.SbiBankRule
import dev.pkdiv.spendtracker.parsing.rules.UpiRule

class ParserEngine(
    rules: List<ParsingRule> = defaultRules(),
) {
    private val rules = rules

    fun parse(sender: String, body: String, rawMessageRef: String): ParseResult {
        val rule = rules.firstOrNull { it.matches(sender, body) }
            ?: return ParseResult.Unrecognized(sender, body, rawMessageRef)
        val transaction = rule.parse(sender, body, rawMessageRef)
            ?: return ParseResult.Unrecognized(sender, body, rawMessageRef)
        return ParseResult.Parsed(transaction)
    }

    companion object {
        fun defaultRules(): List<ParsingRule> = listOf(
            HdfcBankRule(),
            IciciBankRule(),
            SbiBankRule(),
            UpiRule(),
        )
    }
}
