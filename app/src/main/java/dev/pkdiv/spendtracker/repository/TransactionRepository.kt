package dev.pkdiv.spendtracker.repository

import dev.pkdiv.spendtracker.data.db.TransactionDao
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageDao
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageEntity
import dev.pkdiv.spendtracker.ingestion.email.EmailMessage
import dev.pkdiv.spendtracker.parsing.ParsedTransaction
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.util.Privacy
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val unrecognizedMessageDao: UnrecognizedMessageDao,
) {
    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeBetween(from: Instant, to: Instant): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(from, to)

    /** Emits null while the row is absent (deleted), otherwise a UI-ready detail model. */
    fun observeTransactionDetail(id: Long): Flow<TransactionDetail?> =
        transactionDao.observeById(id).map { entity -> entity?.toDetail() }

    suspend fun insert(
        parsed: ParsedTransaction,
        receivedAtMillis: Long,
        smsId: Long? = null,
        rawSender: String? = null,
        rawBody: String? = null,
        source: TransactionSource = TransactionSource.SMS,
        sourceRef: String? = null,
        rawSubject: String? = null,
        bodyTruncated: Boolean = false,
    ) {
        val inserted = transactionDao.insert(
            TransactionEntity(
                amount = parsed.amount,
                direction = parsed.direction,
                merchant = parsed.merchant,
                account = parsed.account,
                category = parsed.category,
                timestamp = parsed.timestamp ?: Instant.ofEpochMilli(receivedAtMillis),
                rawMessageRef = parsed.rawMessageRef,
                smsId = smsId,
                rawSender = rawSender,
                rawBody = rawBody,
                source = source,
                sourceRef = sourceRef ?: smsId?.toString(),
                rawSubject = rawSubject,
                receivedAt = receivedAtMillis,
                bodyTruncated = bodyTruncated,
            )
        )
        if (inserted == -1L && rawSender != null && rawBody != null) {
            transactionDao.updateRawMessage(parsed.rawMessageRef, rawSender, rawBody)
        }
    }

    suspend fun insertEmail(
        parsed: ParsedTransaction,
        email: EmailMessage,
        body: Privacy.CapResult,
    ) {
        transactionDao.insert(
            TransactionEntity(
                amount = parsed.amount,
                direction = parsed.direction,
                merchant = parsed.merchant,
                account = parsed.account,
                category = parsed.category,
                timestamp = parsed.timestamp ?: Instant.ofEpochMilli(email.receivedAtMillis),
                rawMessageRef = parsed.rawMessageRef,
                rawSender = email.sender,
                rawBody = body.text,
                source = TransactionSource.EMAIL,
                sourceRef = email.messageId,
                rawSubject = email.subject,
                receivedAt = email.receivedAtMillis,
                bodyTruncated = body.truncated,
            )
        )
    }

    suspend fun insertUnrecognized(
        sender: String,
        body: String,
        rawMessageRef: String,
        receivedAtMillis: Long,
        smsId: Long? = null,
    ) {
        unrecognizedMessageDao.insert(
            UnrecognizedMessageEntity(
                sender = sender,
                body = body,
                rawMessageRef = rawMessageRef,
                receivedAt = Instant.ofEpochMilli(receivedAtMillis),
                smsId = smsId,
            )
        )
    }

    fun observeUnrecognized(): Flow<List<UnrecognizedMessageEntity>> =
        unrecognizedMessageDao.observeAll()

    suspend fun deleteUnrecognized(id: Long) = unrecognizedMessageDao.delete(id)

    suspend fun updateCategory(id: Long, category: TransactionCategory) =
        transactionDao.updateCategory(id, category)

    private fun TransactionEntity.toDetail() = TransactionDetail(
        id = id,
        amount = amount,
        direction = direction,
        merchant = merchant,
        account = account,
        category = category,
        timestamp = timestamp,
        reference = rawMessageRef,
        source = source ?: TransactionSource.SMS,
        sender = rawSender,
        subject = rawSubject,
        receivedAt = receivedAt?.let(Instant::ofEpochMilli) ?: timestamp,
        body = rawBody,
        bodyTruncated = bodyTruncated ?: false,
    )
}
