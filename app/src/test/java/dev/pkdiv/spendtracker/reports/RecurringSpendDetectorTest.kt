package dev.pkdiv.spendtracker.reports

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecurringSpendDetectorTest {

    private val zone = ZoneOffset.UTC

    private fun txn(
        merchant: String,
        amount: String,
        date: LocalDate,
        direction: TransactionDirection = TransactionDirection.DEBIT,
        category: TransactionCategory = TransactionCategory.OTHER,
        isDuplicate: Boolean = false,
    ) = TransactionEntity(
        amount = BigDecimal(amount),
        direction = direction,
        merchant = merchant,
        account = null,
        category = category,
        timestamp = date.atStartOfDay(zone).toInstant(),
        rawMessageRef = "sms:test-$merchant-$amount-$date-$direction-$isDuplicate",
        isDuplicate = isDuplicate,
    )

    @Test
    fun `detects a monthly subscription`() {
        val transactions = listOf(
            txn("Netflix", "499.00", LocalDate.of(2026, 1, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 2, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 3, 5)),
        )

        val result = RecurringSpendDetector.detect(transactions, LocalDate.of(2026, 3, 20), zone)

        assertEquals(1, result.size)
        assertEquals(RecurringCadence.MONTHLY, result.single().cadence)
        assertEquals("Netflix", result.single().merchant)
        assertEquals(BigDecimal("499.00"), result.single().amount)
        assertEquals(3, result.single().occurrences)
    }

    @Test
    fun `detects a weekly cadence`() {
        val transactions = listOf(
            txn("Grocer", "120.00", LocalDate.of(2026, 1, 5)),
            txn("Grocer", "120.00", LocalDate.of(2026, 1, 12)),
            txn("Grocer", "120.00", LocalDate.of(2026, 1, 19)),
        )

        val result = RecurringSpendDetector.detect(transactions, LocalDate.of(2026, 1, 25), zone)

        assertEquals(1, result.size)
        assertEquals(RecurringCadence.WEEKLY, result.single().cadence)
    }

    @Test
    fun `ignores fewer than three occurrences`() {
        val transactions = listOf(
            txn("Netflix", "499.00", LocalDate.of(2026, 1, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 2, 5)),
        )

        assertTrue(RecurringSpendDetector.detect(transactions, LocalDate.of(2026, 2, 20), zone).isEmpty())
    }

    @Test
    fun `ignores irregular gaps`() {
        val transactions = listOf(
            txn("Netflix", "499.00", LocalDate.of(2026, 1, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 2, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 4, 5)),
        )

        assertTrue(RecurringSpendDetector.detect(transactions, LocalDate.of(2026, 4, 20), zone).isEmpty())
    }

    @Test
    fun `ignores inactive subscriptions`() {
        val transactions = listOf(
            txn("Netflix", "499.00", LocalDate.of(2025, 10, 5)),
            txn("Netflix", "499.00", LocalDate.of(2025, 11, 5)),
            txn("Netflix", "499.00", LocalDate.of(2025, 12, 5)),
        )

        assertTrue(RecurringSpendDetector.detect(transactions, LocalDate.of(2026, 2, 20), zone).isEmpty())
    }

    @Test
    fun `excludes duplicates, self transfers and credits`() {
        val transactions = listOf(
            txn("Netflix", "499.00", LocalDate.of(2026, 1, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 2, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 3, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 3, 5), isDuplicate = true),
            txn("Netflix", "499.00", LocalDate.of(2026, 3, 5), direction = TransactionDirection.CREDIT),
            txn("Netflix", "499.00", LocalDate.of(2026, 3, 5), category = TransactionCategory.SELF_TRANSFER),
        )

        val result = RecurringSpendDetector.detect(transactions, LocalDate.of(2026, 3, 20), zone)

        assertEquals(1, result.size)
        assertEquals(3, result.single().occurrences)
    }

    @Test
    fun `groups by merchant and amount`() {
        val transactions = listOf(
            txn("Netflix", "499.00", LocalDate.of(2026, 1, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 2, 5)),
            txn("Netflix", "499.00", LocalDate.of(2026, 3, 5)),
            txn("Netflix", "999.00", LocalDate.of(2026, 1, 5)),
            txn("Netflix", "999.00", LocalDate.of(2026, 2, 5)),
            txn("Netflix", "999.00", LocalDate.of(2026, 3, 5)),
        )

        val result = RecurringSpendDetector.detect(transactions, LocalDate.of(2026, 3, 20), zone)

        assertEquals(2, result.size)
    }
}
