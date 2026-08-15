package dev.pkdiv.spendtracker.reports

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReportAggregatorTest {

    private fun txn(
        amount: String,
        merchant: String,
        category: TransactionCategory = TransactionCategory.OTHER,
        direction: TransactionDirection = TransactionDirection.DEBIT,
    ) = TransactionEntity(
        amount = BigDecimal(amount),
        direction = direction,
        merchant = merchant,
        account = null,
        category = category,
        timestamp = Instant.now(),
        rawMessageRef = "sms:test",
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
    fun `empty transactions produce zero report`() {
        val report = ReportAggregator.daily(emptyList())
        assertEquals(BigDecimal.ZERO, report.totalSpend)
        assertEquals(0, report.transactionCount)
        assertEquals(emptyList<CategorySpend>(), report.categories)
    }
}
