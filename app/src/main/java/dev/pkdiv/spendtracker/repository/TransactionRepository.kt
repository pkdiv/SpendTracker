package dev.pkdiv.spendtracker.repository

import dev.pkdiv.spendtracker.data.db.TransactionDao
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageDao
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageEntity
import dev.pkdiv.spendtracker.parsing.ParsedTransaction
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val unrecognizedMessageDao: UnrecognizedMessageDao,
) {
    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeBetween(from: Instant, to: Instant): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(from, to)

    suspend fun insert(parsed: ParsedTransaction, receivedAtMillis: Long) {
        transactionDao.insert(
            TransactionEntity(
                amount = parsed.amount,
                direction = parsed.direction,
                merchant = parsed.merchant,
                account = parsed.account,
                category = parsed.category,
                timestamp = parsed.timestamp ?: Instant.ofEpochMilli(receivedAtMillis),
                rawMessageRef = parsed.rawMessageRef,
            )
        )
    }

    suspend fun insertUnrecognized(
        sender: String,
        body: String,
        rawMessageRef: String,
        receivedAtMillis: Long,
    ) {
        unrecognizedMessageDao.insert(
            UnrecognizedMessageEntity(
                sender = sender,
                body = body,
                rawMessageRef = rawMessageRef,
                receivedAt = Instant.ofEpochMilli(receivedAtMillis),
            )
        )
    }

    fun observeUnrecognized(): Flow<List<UnrecognizedMessageEntity>> =
        unrecognizedMessageDao.observeAll()

    suspend fun deleteUnrecognized(id: Long) = unrecognizedMessageDao.delete(id)

    suspend fun updateCategory(id: Long, category: TransactionCategory) =
        transactionDao.updateCategory(id, category)
}
