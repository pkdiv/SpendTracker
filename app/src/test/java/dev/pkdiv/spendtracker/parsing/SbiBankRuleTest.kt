package dev.pkdiv.spendtracker.parsing

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SbiBankRuleTest {

    private val engine = ParserEngine()

    @Test
    fun `parses SBI debit message`() {
        val body = "Dear Customer, Your A/c XX1234 has been debited by Rs.1,250.00 at AMAZON " +
            "on 12-08-2026. Avl Bal Rs 5,000.00."
        val result = engine.parse("SBIINB", body, "sms:10")

        assertTrue(result is ParseResult.Parsed)
        val txn = (result as ParseResult.Parsed).transaction
        assertEquals(TransactionDirection.DEBIT, txn.direction)
        assertEquals(BigDecimal("1250.00"), txn.amount)
        assertEquals("AMAZON", txn.merchant)
        assertEquals("1234", txn.account)
        assertNotNull(txn.timestamp)
    }

    @Test
    fun `parses SBI credit message`() {
        val body = "Your A/c XX1234 has been credited by Rs.5,000.00 on 12-08-2026."
        val result = engine.parse("SBIINB", body, "sms:11")

        assertTrue(result is ParseResult.Parsed)
        val txn = (result as ParseResult.Parsed).transaction
        assertEquals(TransactionDirection.CREDIT, txn.direction)
        assertEquals(BigDecimal("5000.00"), txn.amount)
    }

    @Test
    fun `returns unrecognized for non-transaction message`() {
        val result = engine.parse("SBIINB", "Your OTP is 123456.", "sms:12")

        assertTrue(result is ParseResult.Unrecognized)
    }
}
