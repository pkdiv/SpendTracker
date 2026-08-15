package dev.pkdiv.spendtracker.parsing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ParserEngineTest {

    private val engine = ParserEngine()

    @Test
    fun `routes ICICI message to ICICI rule`() {
        val body = "ICICI Bank: Rs 800.00 debited from card xx5678 at Swiggy on 12-08-2026 20:15:00."
        val result = engine.parse("ICICIB", body, "sms:6")

        assertTrue(result is ParseResult.Parsed)
        val txn = (result as ParseResult.Parsed).transaction
        assertEquals("5678", txn.account)
        assertEquals("Swiggy", txn.merchant)
    }

    @Test
    fun `routes SBI message to SBI rule`() {
        val body = "SBI: Your a/c xx9999 debited Rs 1,500.00 at BigBasket on 12-08-2026 13:00:00."
        val result = engine.parse("SBI", body, "sms:7")

        assertTrue(result is ParseResult.Parsed)
        val txn = (result as ParseResult.Parsed).transaction
        assertEquals("9999", txn.account)
    }

    @Test
    fun `unrecognized when amount is missing`() {
        val body = "ICICI Bank: debited from card xx5678 at Swiggy."
        val result = engine.parse("ICICIB", body, "sms:8")
        assertTrue(result is ParseResult.Unrecognized)
    }
}
