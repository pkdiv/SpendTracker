package dev.pkdiv.spendtracker.widget

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.reports.ReportAggregator
import java.math.BigDecimal

/** Glanceable month-to-date spend shown by the home-screen widget. */
data class WidgetSpend(
    val totalSpend: BigDecimal,
    val transactionCount: Int,
) {
    companion object {
        /**
         * Same aggregation the Home hero uses: debits only, excluding self transfers
         * and duplicates. The window (current month) is applied by the caller.
         */
        fun from(transactions: List<TransactionEntity>): WidgetSpend {
            val report = ReportAggregator.monthly(transactions, emptyList())
            return WidgetSpend(
                totalSpend = report.totalSpend,
                transactionCount = report.transactionCount,
            )
        }
    }
}
