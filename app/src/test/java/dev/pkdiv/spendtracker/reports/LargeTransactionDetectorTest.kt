package dev.pkdiv.spendtracker.reports

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LargeTransactionDetectorTest {

    private fun txn(amount: String, merchant: String = "Merchant") = TransactionEntity(
        amount = BigDecimal(amount),
        direction = TransactionDirection.DEBIT,
        merchant = merchant,
        account = null,
        category = TransactionCategory.OTHER,
        timestamp = Instant.now(),
        rawMessageRef = "sms:test-$amount-$merchant",
    )

    @Test
    fun `flags a clear outlier above the median threshold`() {
        val transactions = listOf(
            txn("100.00"),
            txn("110.00"),
            txn("95.00"),
            txn("105.00"),
            txn("1000.00", merchant = "Big purchase"),
        )

        val flagged = LargeTransactionDetector.detect(transactions)

        assertEquals(1, flagged.size)
        assertEquals("Big purchase", flagged.single().merchant)
        assertEquals(BigDecimal("1000.00"), flagged.single().amount)
    }

    @Test
    fun `does not flag uniform transactions`() {
        val transactions = (1..6).map { txn("100.00") }

        assertTrue(LargeTransactionDetector.detect(transactions).isEmpty())
    }

    @Test
    fun `returns empty when the sample is too small`() {
        val transactions = listOf(txn("10.00"), txn("1000.00"))

        assertTrue(LargeTransactionDetector.detect(transactions).isEmpty())
    }

    @Test
    fun `caps flags to five and sorts by amount descending`() {
        val transactions = listOf(
            txn("100.00"),
            txn("100.00"),
            txn("100.00"),
            txn("100.00"),
            txn("100.00"),
            txn("100.00"),
            txn("100.00"),
            txn("400.00"),
            txn("500.00"),
            txn("600.00"),
            txn("700.00"),
            txn("800.00"),
            txn("900.00"),
        )

        val flagged = LargeTransactionDetector.detect(transactions)

        assertEquals(5, flagged.size)
        assertEquals(
            listOf("900.00", "800.00", "700.00", "600.00", "500.00"),
            flagged.map { it.amount.toPlainString() },
        )
    }

    @Test
    fun `median handles even sample sizes`() {
        val transactions = listOf(
            txn("100.00"),
            txn("120.00"),
            txn("150.00"),
            txn("180.00"),
            txn("200.00"),
            txn("1000.00"),
        )

        val flagged = LargeTransactionDetector.detect(transactions)

        assertEquals(1, flagged.size)
        assertEquals(BigDecimal("1000.00"), flagged.single().amount)
    }
}
