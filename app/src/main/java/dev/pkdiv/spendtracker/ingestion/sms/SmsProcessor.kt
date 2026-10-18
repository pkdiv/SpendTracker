package dev.pkdiv.spendtracker.ingestion.sms

import dev.pkdiv.spendtracker.parsing.ParseResult
import dev.pkdiv.spendtracker.parsing.ParserEngine
import dev.pkdiv.spendtracker.repository.TransactionRepository
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
        return when (val result = parser.parse(sender, body, rawMessageRef)) {
            is ParseResult.Parsed -> {
                transactionRepository.insert(result.transaction, receivedAtMillis, smsId, sender, body)
                widgetRefresher.refresh()
                result
            }
            is ParseResult.Unrecognized -> {
                transactionRepository.insertUnrecognized(sender, body, rawMessageRef, receivedAtMillis, smsId)
                result
            }
        }
    }
}
