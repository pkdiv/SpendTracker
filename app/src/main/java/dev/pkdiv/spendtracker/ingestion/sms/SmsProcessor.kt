package dev.pkdiv.spendtracker.ingestion.sms

import dev.pkdiv.spendtracker.parsing.ParseResult
import dev.pkdiv.spendtracker.parsing.ParserEngine
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.util.Privacy
import dev.pkdiv.spendtracker.widget.WidgetRefresher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsProcessor @Inject constructor(
    private val parser: ParserEngine,
    private val transactionRepository: TransactionRepository,
    private val widgetRefresher: WidgetRefresher,
) {
    suspend fun process(
        sender: String,
        body: String,
        rawMessageRef: String,
        receivedAtMillis: Long,
        smsId: Long? = null,
    ): ParseResult {
        // Unrelated personal messages never reach the database (AGENTS §3).
        if (!SmsFilter.isTransactionShaped(sender, body)) return ParseResult.Ignored

        val capped = Privacy.capBody(body)
        return when (val result = parser.parse(sender, capped.text, rawMessageRef)) {
            is ParseResult.Parsed -> {
                transactionRepository.insert(
                    parsed = result.transaction,
                    receivedAtMillis = receivedAtMillis,
                    smsId = smsId,
                    rawSender = sender,
                    rawBody = capped.text,
                    bodyTruncated = capped.truncated,
                )
                widgetRefresher.refresh()
                result
            }
            is ParseResult.Unrecognized -> {
                transactionRepository.insertUnrecognized(
                    sender = sender,
                    body = capped.text,
                    rawMessageRef = rawMessageRef,
                    receivedAtMillis = receivedAtMillis,
                    smsId = smsId,
                )
                result
            }
            is ParseResult.Ignored -> result
        }
    }
}
