package dev.pkdiv.spendtracker.data.db

/** Pure matching logic for near-duplicate transaction messages. */
object DuplicateDetector {

    const val WINDOW_MILLIS = 120_000L

    fun isSameTransaction(existing: TransactionEntity, incoming: TransactionEntity): Boolean =
        existing.amount == incoming.amount &&
            existing.direction == incoming.direction &&
            existing.merchant.trim().equals(incoming.merchant.trim(), ignoreCase = true)
}
