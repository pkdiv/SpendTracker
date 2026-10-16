package dev.pkdiv.spendtracker.data.db

import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DuplicateDetectorTest {

    private fun txn(
        amount: String = "100.00",
        direction: TransactionDirection = TransactionDirection.DEBIT,
        merchant: String = "Swiggy",
    ) = TransactionEntity(
        amount = BigDecimal(amount),
        direction = direction,
        merchant = merchant,
        account = null,
        category = TransactionCategory.OTHER,
        timestamp = Instant.now(),
        rawMessageRef = "sms:test",
    )

    @Test
    fun `same amount direction and merchant match`() {
        assertTrue(DuplicateDetector.isSameTransaction(txn(), txn()))
    }

    @Test
    fun `different amount does not match`() {
        assertFalse(DuplicateDetector.isSameTransaction(txn(), txn(amount = "200.00")))
    }

    @Test
    fun `different direction does not match`() {
        assertFalse(
            DuplicateDetector.isSameTransaction(
                txn(),
                txn(direction = TransactionDirection.CREDIT),
            ),
        )
    }

    @Test
    fun `merchant case and whitespace differences still match`() {
        assertTrue(
            DuplicateDetector.isSameTransaction(
                txn(merchant = " swiggy "),
                txn(merchant = "SWIGGY"),
            ),
        )
    }

    @Test
    fun `different merchant does not match`() {
        assertFalse(DuplicateDetector.isSameTransaction(txn(), txn(merchant = "Amazon")))
    }
}
