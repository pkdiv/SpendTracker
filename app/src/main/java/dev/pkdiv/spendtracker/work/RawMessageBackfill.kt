package dev.pkdiv.spendtracker.work

import dev.pkdiv.spendtracker.data.db.TransactionDao
import dev.pkdiv.spendtracker.ingestion.sms.SmsBodyResolver
import javax.inject.Inject

/**
 * One-time, idempotent, resumable backfill of SMS bodies for rows that lack them.
 * Iterates in batches; a re-run continues where the previous run stopped, and a batch
 * that resolves nothing stops the run so unresolvable rows don't loop forever.
 */
class RawMessageBackfill @Inject constructor(
    private val transactionDao: TransactionDao,
    private val smsBodyResolver: SmsBodyResolver,
) {
    /**
     * @return true when there is nothing left to resolve (including rows that can't be
     *         resolved), false when SMS permission is revoked.
     */
    suspend fun run(hasSmsPermission: Boolean, batchSize: Int = DEFAULT_BATCH_SIZE): Boolean {
        if (!hasSmsPermission) return false
        while (true) {
            val pending = transactionDao.pendingRawMessages(batchSize)
            if (pending.isEmpty()) return true

            var anyResolved = false
            pending.forEach { transaction ->
                val smsId = transaction.smsId ?: return@forEach
                val resolved = smsBodyResolver.resolve(smsId) ?: return@forEach
                transactionDao.updateRawMessageById(
                    id = transaction.id,
                    sender = resolved.sender,
                    body = resolved.body,
                    truncated = resolved.truncated,
                )
                anyResolved = true
            }
            // Nothing in this batch could be resolved; the rest won't be either.
            if (!anyResolved) return true
        }
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 100
    }
}
