package dev.pkdiv.spendtracker.parsing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HdfcBankRuleTest {

    private val engine = ParserEngine()

    @Test
    fun `parses HDFC debit message`() {
        val body = "Dear Customer, Rs 1,250.00 debited from your HDFC Bank account xx1234 at AMAZON on 12-08-2026 14:30:05. Avl bal Rs 5,000.00."
        val result = engine.parse("HDFC-BANK", body, "sms:1")

        assertTrue(result is ParseResult.Parsed)
        val txn = (result as ParseResult.Parsed).transaction
        assertEquals(TransactionDirection.DEBIT, txn.direction)
        assertEquals(java.math.BigDecimal("1250.00"), txn.amount)
        assertEquals("AMAZON", txn.merchant)
        assertEquals("1234", txn.account)
        assertNotNull(txn.timestamp)
    }

    @Test
    fun `parses HDFC credit message`() {
        val body = "Dear Customer, Rs 5,000.00 credited to your HDFC Bank account xx1234 on 12-08-2026 09:00:00."
        val result = engine.parse("HDFC-BANK", body, "sms:2")

        assertTrue(result is ParseResult.Parsed)
        val txn = (result as ParseResult.Parsed).transaction
        assertEquals(TransactionDirection.CREDIT, txn.direction)
        assertEquals(java.math.BigDecimal("5000.00"), txn.amount)
    }

    @Test
    fun `returns unrecognized for non-transaction message`() {
        val body = "Your OTP is 123456."
        val result = engine.parse("HDFC-BANK", body, "sms:3")
        assertTrue(result is ParseResult.Unrecognized)
    }
}
