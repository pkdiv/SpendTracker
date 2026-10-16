package dev.pkdiv.spendtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE rawBody IS NULL AND source = 'SMS' AND smsId IS NOT NULL LIMIT :limit")
    suspend fun pendingRawMessages(limit: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun observeBetween(from: Instant, to: Instant): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    suspend fun between(from: Instant, to: Instant): List<TransactionEntity>

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: TransactionCategory)

    @Query(
        """
        SELECT * FROM transactions
        WHERE category = 'OTHER' AND id > :afterId
        ORDER BY id ASC
        LIMIT :limit
        """,
    )
    suspend fun otherCategoryTransactions(afterId: Long, limit: Int): List<TransactionEntity>

    @Query("UPDATE transactions SET rawSender = :sender, rawBody = :body WHERE rawMessageRef = :rawMessageRef")
    suspend fun updateRawMessage(rawMessageRef: String, sender: String, body: String)

    @Query("UPDATE transactions SET rawSender = :sender, rawBody = :body, bodyTruncated = :truncated WHERE id = :id")
    suspend fun updateRawMessageById(id: Long, sender: String, body: String, truncated: Boolean)

    @Query(
        """
        SELECT * FROM transactions
        WHERE rawSender = :sender
          AND direction = :direction
          AND receivedAt >= :fromMillis AND receivedAt <= :toMillis
          AND isDuplicate = 0
        ORDER BY receivedAt ASC
        """,
    )
    suspend fun recentCandidates(
        sender: String,
        direction: TransactionDirection,
        fromMillis: Long,
        toMillis: Long,
    ): List<TransactionEntity>

    @Query("UPDATE transactions SET isDuplicate = :isDuplicate WHERE id = :id")
    suspend fun setDuplicate(id: Long, isDuplicate: Boolean)

    @Transaction
    suspend fun insertDeduplicated(transaction: TransactionEntity, windowMillis: Long): Long {
        val sender = transaction.rawSender
        val receivedAt = transaction.receivedAt
        if (sender == null || receivedAt == null) return insert(transaction)

        val isDuplicate = recentCandidates(
            sender = sender,
            direction = transaction.direction,
            fromMillis = receivedAt - windowMillis,
            toMillis = receivedAt + windowMillis,
        ).any { DuplicateDetector.isSameTransaction(it, transaction) }

        return insert(if (isDuplicate) transaction.copy(isDuplicate = true) else transaction)
    }
}
