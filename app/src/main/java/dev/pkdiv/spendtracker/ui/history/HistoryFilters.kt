package dev.pkdiv.spendtracker.ui.history

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/** Search + filter criteria selected on the Activity screen. */
data class HistoryFilters(
    val query: String = "",
    val category: TransactionCategory? = null,
    val direction: TransactionDirection? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val minAmountInput: String = "",
    val maxAmountInput: String = "",
    val excludeDuplicates: Boolean = false,
) {
    val minAmount: BigDecimal? get() = minAmountInput.trim().toBigDecimalOrNull()
    val maxAmount: BigDecimal? get() = maxAmountInput.trim().toBigDecimalOrNull()

    /** Number of active filters (the search query is tracked separately). */
    val activeFilterCount: Int
        get() = listOf(
            category != null,
            direction != null,
            fromDate != null || toDate != null,
            minAmountInput.isNotBlank() || maxAmountInput.isNotBlank(),
            excludeDuplicates,
        ).count { it }

    val hasAnyCriteria: Boolean get() = activeFilterCount > 0 || query.isNotBlank()
}

/** Pure, in-memory filtering so the logic is unit-testable without Room or a device. */
fun filterTransactions(
    transactions: List<TransactionEntity>,
    filters: HistoryFilters,
    zone: ZoneId,
): List<TransactionEntity> {
    val min = filters.minAmount
    val max = filters.maxAmount
    return transactions.filter { transaction ->
        val date = transaction.timestamp.atZone(zone).toLocalDate()
        val matchesQuery = filters.query.isBlank() ||
            transaction.merchant.contains(filters.query, ignoreCase = true) ||
            transaction.amount.toPlainString().contains(filters.query)
        val matchesCategory = filters.category == null || transaction.category == filters.category
        val matchesDirection = filters.direction == null || transaction.direction == filters.direction
        val matchesFrom = filters.fromDate == null || !date.isBefore(filters.fromDate)
        val matchesTo = filters.toDate == null || !date.isAfter(filters.toDate)
        val matchesMin = min == null || transaction.amount >= min
        val matchesMax = max == null || transaction.amount <= max
        val matchesDuplicate = !filters.excludeDuplicates || !transaction.isDuplicate

        matchesQuery && matchesCategory && matchesDirection && matchesFrom && matchesTo &&
            matchesMin && matchesMax && matchesDuplicate
    }
}
