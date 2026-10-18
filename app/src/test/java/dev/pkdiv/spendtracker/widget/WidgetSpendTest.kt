package dev.pkdiv.spendtracker.widget

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WidgetSpendTest {

    private fun txn(
        amount: String,
        direction: TransactionDirection = TransactionDirection.DEBIT,
        category: TransactionCategory = TransactionCategory.FOOD,
        isDuplicate: Boolean = false,
    ) = TransactionEntity(
        amount = BigDecimal(amount),
        direction = direction,
        merchant = "Merchant",
        account = null,
        category = category,
        timestamp = Instant.parse("2026-10-14T10:00:00Z"),
        rawMessageRef = "sms:test",
        isDuplicate = isDuplicate,
    )

    @Test
    fun `sums debits only excluding self transfers and duplicates`() {
        val spend = WidgetSpend.from(
            listOf(
                txn("100.00"),
                txn("50.00", category = TransactionCategory.TRANSPORT),
                txn("999.00", direction = TransactionDirection.CREDIT),
                txn("500.00", category = TransactionCategory.SELF_TRANSFER),
                txn("100.00", isDuplicate = true),
            ),
        )

        assertEquals(BigDecimal("150.00"), spend.totalSpend)
        assertEquals(4, spend.transactionCount)
    }

    @Test
    fun `empty transactions produce a zero widget`() {
        val spend = WidgetSpend.from(emptyList())

        assertEquals(BigDecimal.ZERO, spend.totalSpend)
        assertEquals(0, spend.transactionCount)
    }
}
