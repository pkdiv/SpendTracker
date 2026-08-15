package dev.pkdiv.spendtracker.parsing

sealed interface ParseResult {
    data class Parsed(val transaction: ParsedTransaction) : ParseResult
    data class Unrecognized(val sender: String, val body: String, val rawMessageRef: String) : ParseResult
}
