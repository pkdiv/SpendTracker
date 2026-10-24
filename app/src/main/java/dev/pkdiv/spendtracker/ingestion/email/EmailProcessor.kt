package dev.pkdiv.spendtracker.ingestion.email

import dev.pkdiv.spendtracker.parsing.ParseResult
import dev.pkdiv.spendtracker.parsing.ParserEngine
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.util.HtmlToText
import dev.pkdiv.spendtracker.util.Privacy
import javax.inject.Inject
import javax.inject.Singleton

/** Parses and persists an email's plain-text body (HTML is converted, never rendered). */
@Singleton
class EmailProcessor @Inject constructor(
    private val parser: ParserEngine,
    private val transactionRepository: TransactionRepository,
) {
    suspend fun process(message: EmailMessage): ParseResult {
        val body = Privacy.capBody(HtmlToText.convert(message.body))
        return when (val result = parser.parse(message.sender, body.text, message.rawMessageRef)) {
            is ParseResult.Parsed -> {
                transactionRepository.insertEmail(result.transaction, message, body)
                result
            }

            is ParseResult.Unrecognized -> {
                transactionRepository.insertUnrecognized(
                    sender = message.sender,
                    body = body.text,
                    rawMessageRef = message.rawMessageRef,
                    receivedAtMillis = message.receivedAtMillis,
                )
                result
            }

            is ParseResult.Ignored -> result
        }
    }
}
