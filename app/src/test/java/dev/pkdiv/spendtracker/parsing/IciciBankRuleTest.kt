package dev.pkdiv.spendtracker.parsing

import dev.pkdiv.spendtracker.parsing.rules.IciciBankRule
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IciciBankRuleTest {

    private val rule = IciciBankRule()

    @Test
    fun `parses real ICICI spend message`() {
        val body = "INR 268.00 spent using ICICI Bank Card XX6002 on 13-Aug-26 on AMAZON PAY IN G. Avl Limit: INR 32,891.97. If not you, call 1800 2662/SMS BLOCK 6002 to 9215676766."
        val result = rule.parse("ICICIB", body, "sms:1")

        assertNotNull(result)
        assertEquals(BigDecimal("268.00"), result!!.amount)
        assertEquals(TransactionDirection.DEBIT, result.direction)
        assertEquals("AMAZON PAY IN G", result.merchant)
        assertEquals("6002", result.account)
        assertNotNull(result.timestamp)
    }

    @Test
    fun `uses USD spend amount instead of INR available balance`() {
        val body = "USD 12.34 spent using ICICI Bank Card XX6002 on 02-Aug-26 on CLOUDFLARE. Avl Limit: INR 21,069.11. If not you, call 1800 2662/SMS BLOCK 6002 to 9215676766."
        val result = rule.parse("ICICIB", body, "sms:2")

        assertNotNull(result)
        assertEquals(BigDecimal("12.34"), result!!.amount)
        assertEquals(TransactionDirection.DEBIT, result.direction)
        assertEquals("CLOUDFLARE", result.merchant)
        assertEquals("6002", result.account)
    }
}
