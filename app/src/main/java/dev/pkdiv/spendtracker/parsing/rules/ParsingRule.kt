package dev.pkdiv.spendtracker.parsing.rules

import dev.pkdiv.spendtracker.parsing.ParsedTransaction

interface ParsingRule {
    val id: String

    fun matches(sender: String, body: String): Boolean

    fun parse(sender: String, body: String, rawMessageRef: String): ParsedTransaction?
}
