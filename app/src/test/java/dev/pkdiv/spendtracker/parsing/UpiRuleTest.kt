package dev.pkdiv.spendtracker.parsing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpiRuleTest {

    private val engine = ParserEngine()

    @Test
    fun `parses UPI debit message`() {
        val body = "UPI txn of Rs 250.00 to merchant@upi on 12-08-2026 18:45:10 debited from your account."
        val result = engine.parse("UPI", body, "sms:4")

        assertTrue(result is ParseResult.Parsed)
        val txn = (result as ParseResult.Parsed).transaction
        assertEquals(TransactionDirection.DEBIT, txn.direction)
        assertEquals(java.math.BigDecimal("250.00"), txn.amount)
        assertEquals("merchant@upi", txn.merchant)
    }

    @Test
    fun `parses UPI credit message`() {
        val body = "Rs 1,000.00 credited via UPI from friend@upi on 12-08-2026 10:00:00."
        val result = engine.parse("UPI", body, "sms:5")

        assertTrue(result is ParseResult.Parsed)
        val txn = (result as ParseResult.Parsed).transaction
        assertEquals(TransactionDirection.CREDIT, txn.direction)
    }
}
