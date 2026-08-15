package dev.pkdiv.spendtracker.reports

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal

object ReportAggregator {

    fun daily(transactions: List<TransactionEntity>): DailyReport {
        val debits = transactions.filter { it.direction == TransactionDirection.DEBIT }
        return DailyReport(
            totalSpend = debits.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount },
            transactionCount = transactions.size,
            top = topMerchants(debits, 3),
            categories = byCategory(debits),
        )
    }

    fun monthly(transactions: List<TransactionEntity>, previousTransactions: List<TransactionEntity>): MonthlyReport {
        val debits = transactions.filter { it.direction == TransactionDirection.DEBIT }
        val prevDebits = previousTransactions.filter { it.direction == TransactionDirection.DEBIT }
        return MonthlyReport(
            totalSpend = debits.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount },
            transactionCount = transactions.size,
            categories = byCategory(debits),
            topMerchants = topMerchants(debits, 5),
            previousMonthTotal = prevDebits.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount },
        )
    }

    private fun topMerchants(transactions: List<TransactionEntity>, limit: Int): List<MerchantSpend> =
        transactions
            .groupBy { it.merchant }
            .map { (merchant, txns) ->
                MerchantSpend(merchant, txns.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount })
            }
            .sortedByDescending { it.total }
            .take(limit)

    private fun byCategory(transactions: List<TransactionEntity>): List<CategorySpend> =
        transactions
            .groupBy { it.category }
            .map { (category, txns) ->
                CategorySpend(category, txns.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount })
            }
            .sortedByDescending { it.total }
}
