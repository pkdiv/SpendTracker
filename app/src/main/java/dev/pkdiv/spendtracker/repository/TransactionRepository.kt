package dev.pkdiv.spendtracker.repository

import dev.pkdiv.spendtracker.data.db.DuplicateDetector
import dev.pkdiv.spendtracker.data.db.MerchantCategoryDao
import dev.pkdiv.spendtracker.data.db.MerchantCategoryEntity
import dev.pkdiv.spendtracker.data.db.TransactionDao
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageDao
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageEntity
import dev.pkdiv.spendtracker.ingestion.email.EmailMessage
import dev.pkdiv.spendtracker.parsing.ParsedTransaction
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionCategorizer
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.util.MerchantKey
import dev.pkdiv.spendtracker.util.Privacy
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val unrecognizedMessageDao: UnrecognizedMessageDao,
    private val merchantCategoryDao: MerchantCategoryDao,
    private val categorizer: TransactionCategorizer,
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
        val inserted = transactionDao.insertDeduplicated(
            TransactionEntity(
                amount = parsed.amount,
                direction = parsed.direction,
                merchant = parsed.merchant,
                account = parsed.account,
                category = resolveCategory(parsed.merchant, parsed.category),
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
            ),
            DuplicateDetector.WINDOW_MILLIS,
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
                category = resolveCategory(parsed.merchant, parsed.category),
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

    /**
     * Stores a hand-entered transaction (cash/offline spending). Persisted with a
     * synthetic reference and [TransactionSource.MANUAL]; the category is stored as
     * chosen rather than re-derived, and it bypasses SMS duplicate detection.
     */
    suspend fun insertManual(
        amount: BigDecimal,
        direction: TransactionDirection,
        merchant: String,
        category: TransactionCategory,
        account: String?,
        timestamp: Instant,
    ): Long = transactionDao.insert(
        TransactionEntity(
            amount = amount,
            direction = direction,
            merchant = merchant,
            account = account,
            category = category,
            timestamp = timestamp,
            rawMessageRef = "manual:${UUID.randomUUID()}",
            source = TransactionSource.MANUAL,
            receivedAt = timestamp.toEpochMilli(),
            isDuplicate = false,
        ),
    )

    suspend fun insertUnrecognized(
        sender: String,
        body: String,
        rawMessageRef: String,
        receivedAtMillis: Long,
        smsId: Long? = null,
    ) {
        // The live receiver and the backfill reader derive rawMessageRef differently, so
        // the same SMS would otherwise be stored twice under two refs. Checking by content
        // also means a message the user dismissed is never re-surfaced by the backfill.
        if (unrecognizedMessageDao.countForContent(sender, receivedAtMillis, body) > 0) return
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

    suspend fun dismissUnrecognized(id: Long) = unrecognizedMessageDao.dismiss(id)

    suspend fun dismissAllUnrecognized() = unrecognizedMessageDao.dismissAll()

    suspend fun updateCategory(id: Long, category: TransactionCategory) =
        transactionDao.updateCategory(id, category)

    suspend fun setDuplicate(id: Long, isDuplicate: Boolean) =
        transactionDao.setDuplicate(id, isDuplicate)

    /**
     * Renames the merchant on one transaction and teaches the app the new name ->
     * this transaction's category, so future transactions from that merchant
     * auto-categorize. Blank names are ignored.
     */
    suspend fun renameMerchant(id: Long, merchant: String) {
        val name = merchant.trim()
        if (name.isEmpty()) return
        transactionDao.updateMerchant(id, name)
        transactionDao.categoryById(id)?.let { setMerchantCategory(name, it) }
    }

    /** Sets or clears the free-text note on a transaction (blank/whitespace clears it to null). */
    suspend fun setNote(id: Long, note: String?) =
        transactionDao.updateNote(id, note?.trim()?.ifBlank { null })

    /** User-taught merchant -> category overrides, ready for the editor UI. */
    fun observeMerchantCategories(): Flow<List<MerchantCategoryMapping>> =
        merchantCategoryDao.observeAll().map { rows ->
            rows.map { row ->
                MerchantCategoryMapping(
                    merchant = row.merchant,
                    category = TransactionCategory.entries.firstOrNull { it.name == row.category }
                        ?: TransactionCategory.OTHER,
                )
            }
        }

    /**
     * Teaches the app that [merchant] belongs to [category]. The merchant key is
     * normalized so the exact-match lookup at categorization time is reliable.
     * A blank merchant is ignored.
     */
    suspend fun setMerchantCategory(merchant: String, category: TransactionCategory) {
        val key = MerchantKey.normalize(merchant)
        if (key.isEmpty()) return
        merchantCategoryDao.upsert(MerchantCategoryEntity(merchant = key, category = category.name))
    }

    suspend fun deleteMerchantCategory(merchant: String) =
        merchantCategoryDao.delete(MerchantKey.normalize(merchant))

    /**
     * Idempotent, resumable backfill for rows left as [TransactionCategory.OTHER] before
     * categorization was wired on insert. Walks candidates in id order in batches and
     * resolves each through the same precedence as insert ([resolveCategory]).
     *
     * Only rows currently in [TransactionCategory.OTHER] are considered, so a transaction
     * the user has since re-categorized is never re-touched. Rows the resolver can't place
     * stay [TransactionCategory.OTHER] and are left alone, making a re-run a no-op.
     *
     * @return the number of rows whose category was changed.
     */
    suspend fun recategorizeOtherTransactions(batchSize: Int = DEFAULT_BATCH_SIZE): Int {
        var afterId = 0L
        var updated = 0
        while (true) {
            val batch = transactionDao.otherCategoryTransactions(afterId, batchSize)
            if (batch.isEmpty()) return updated
            batch.forEach { transaction ->
                val resolved = resolveCategory(transaction.merchant, transaction.category)
                if (resolved != transaction.category) {
                    transactionDao.updateCategory(transaction.id, resolved)
                    updated++
                }
            }
            afterId = batch.last().id
        }
    }

    private suspend fun resolveCategory(merchant: String, fallback: TransactionCategory): TransactionCategory {
        merchantCategoryDao.categoryFor(MerchantKey.normalize(merchant))
            ?.let { value -> TransactionCategory.entries.firstOrNull { it.name == value } }
            ?.let { return it }
        return categorizer.categorize(merchant).takeUnless { it == TransactionCategory.OTHER } ?: fallback
    }

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
        isDuplicate = isDuplicate,
        note = note,
    )

    companion object {
        const val DEFAULT_BATCH_SIZE = 100
    }
}
