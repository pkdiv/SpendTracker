package dev.pkdiv.spendtracker.reports

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReportAggregatorTest {

    private fun txn(
        amount: String,
        merchant: String,
        category: TransactionCategory = TransactionCategory.OTHER,
        direction: TransactionDirection = TransactionDirection.DEBIT,
        isDuplicate: Boolean = false,
        timestamp: Instant = Instant.now(),
    ) = TransactionEntity(
        amount = BigDecimal(amount),
        direction = direction,
        merchant = merchant,
        account = null,
        category = category,
        timestamp = timestamp,
        rawMessageRef = "sms:test",
        isDuplicate = isDuplicate,
    )

    @Test
    fun `daily report sums debit transactions only`() {
        val transactions = listOf(
            txn("100.00", "Merchant A", TransactionCategory.FOOD),
            txn("50.00", "Merchant B", TransactionCategory.TRANSPORT),
            txn("999.00", "Refund", direction = TransactionDirection.CREDIT),
        )

        val report = ReportAggregator.daily(transactions)

        assertEquals(BigDecimal("150.00"), report.totalSpend)
        assertEquals(3, report.transactionCount)
        assertEquals(2, report.categories.size)
        assertEquals("Merchant A", report.top.first().merchant)
    }

    @Test
    fun `monthly report compares previous month`() {
        val current = listOf(
            txn("200.00", "Merchant A", TransactionCategory.SHOPPING),
            txn("50.00", "Merchant B", TransactionCategory.BILLS),
        )
        val previous = listOf(
            txn("75.00", "Merchant C", TransactionCategory.BILLS),
        )

        val report = ReportAggregator.monthly(current, previous)

        assertEquals(BigDecimal("250.00"), report.totalSpend)
        assertEquals(BigDecimal("75.00"), report.previousMonthTotal)
        assertEquals(2, report.topMerchants.size)
    }

    @Test
    fun `self transfer debits are excluded from spend`() {
        val transactions = listOf(
            txn("500.00", "Self account", TransactionCategory.SELF_TRANSFER),
            txn("100.00", "Merchant A", TransactionCategory.FOOD),
        )

        val report = ReportAggregator.daily(transactions)

        assertEquals(BigDecimal("100.00"), report.totalSpend)
        assertEquals(1, report.categories.size)
        assertEquals("Merchant A", report.top.first().merchant)
    }

    @Test
    fun `empty transactions produce zero report`() {
        val report = ReportAggregator.daily(emptyList())
        assertEquals(BigDecimal.ZERO, report.totalSpend)
        assertEquals(0, report.transactionCount)
        assertEquals(emptyList<CategorySpend>(), report.categories)
    }

    @Test
    fun `spendByDay buckets debits by day and drops non-spend`() {
        val zone = ZoneId.of("UTC")
        val day1 = Instant.parse("2026-10-14T10:00:00Z")
        val day2 = Instant.parse("2026-10-15T10:00:00Z")
        val transactions = listOf(
            txn("100.00", "Merchant A", TransactionCategory.FOOD, timestamp = day1),
            txn("50.00", "Merchant B", TransactionCategory.TRANSPORT, timestamp = day1),
            txn("999.00", "Refund", direction = TransactionDirection.CREDIT, timestamp = day1),
            txn("500.00", "Self account", TransactionCategory.SELF_TRANSFER, timestamp = day2),
            txn("25.00", "Duplicate", TransactionCategory.FOOD, isDuplicate = true, timestamp = day2),
            txn("70.00", "Merchant C", TransactionCategory.SHOPPING, timestamp = day2),
        )

        val byDay = ReportAggregator.spendByDay(transactions, zone)

        assertEquals(BigDecimal("150.00"), byDay[LocalDate.of(2026, 10, 14)])
        assertEquals(BigDecimal("70.00"), byDay[LocalDate.of(2026, 10, 15)])
        assertEquals(2, byDay.size)
    }

    @Test
    fun `duplicate transactions are excluded from totals and counts`() {
        val transactions = listOf(
            txn("100.00", "Merchant A", TransactionCategory.FOOD),
            txn("100.00", "Merchant A", TransactionCategory.FOOD, isDuplicate = true),
            txn("50.00", "Merchant B", TransactionCategory.TRANSPORT),
        )

        val report = ReportAggregator.daily(transactions)

        assertEquals(BigDecimal("150.00"), report.totalSpend)
        assertEquals(2, report.transactionCount)
        assertEquals(2, report.categories.size)
        assertEquals(BigDecimal("100.00"), report.top.first().total)
    }
}
