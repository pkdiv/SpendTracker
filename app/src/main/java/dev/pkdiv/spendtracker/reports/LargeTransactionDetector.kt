package dev.pkdiv.spendtracker.reports

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Flags transactions that are unusually large for the month they appear in.
 *
 * The threshold is a statistic of the month itself: three times the median
 * transaction amount. Median (rather than mean) keeps the threshold stable even
 * when the month contains the very outlier being flagged. No server-side or
 * machine-learning logic — this is a deterministic, explainable heuristic.
 */
object LargeTransactionDetector {

    private const val MIN_SAMPLE_SIZE = 5
    private const val THRESHOLD_MULTIPLIER = 3
    private const val MAX_FLAGS = 5

    /**
     * [transactions] must already be reduced to spend (debits only, minus
     * self-transfers and duplicates) by [ReportAggregator].
     */
    fun detect(transactions: List<TransactionEntity>): List<FlaggedTransaction> {
        if (transactions.size < MIN_SAMPLE_SIZE) return emptyList()

        val median = median(transactions.map { it.amount })
        if (median.signum() <= 0) return emptyList()
        val threshold = median.multiply(BigDecimal(THRESHOLD_MULTIPLIER))

        return transactions
            .filter { it.amount.compareTo(threshold) > 0 }
            .sortedByDescending { it.amount }
            .take(MAX_FLAGS)
            .map {
                FlaggedTransaction(
                    merchant = it.merchant,
                    amount = it.amount,
                    category = it.category,
                    timestamp = it.timestamp,
                )
            }
    }

    private fun median(amounts: List<BigDecimal>): BigDecimal {
        val sorted = amounts.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            sorted[mid - 1].add(sorted[mid]).divide(BigDecimal(2), 2, RoundingMode.HALF_UP)
        }
    }
}
