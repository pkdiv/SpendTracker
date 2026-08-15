package dev.pkdiv.spendtracker.ingestion.sms

import dev.pkdiv.spendtracker.parsing.ParseResult
import dev.pkdiv.spendtracker.parsing.ParserEngine
import dev.pkdiv.spendtracker.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsProcessor @Inject constructor(
    private val parser: ParserEngine,
    private val transactionRepository: TransactionRepository,
) {
    suspend fun process(
        sender: String,
        body: String,
        rawMessageRef: String,
        receivedAtMillis: Long,
    ): ParseResult {
        return when (val result = parser.parse(sender, body, rawMessageRef)) {
            is ParseResult.Parsed -> {
                transactionRepository.insert(result.transaction, receivedAtMillis)
                result
            }
            is ParseResult.Unrecognized -> {
                transactionRepository.insertUnrecognized(sender, body, rawMessageRef, receivedAtMillis)
                result
            }
        }
    }
}
