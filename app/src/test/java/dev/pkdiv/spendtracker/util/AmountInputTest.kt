package dev.pkdiv.spendtracker.util

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AmountInputTest {

    @Test
    fun `sanitize keeps digits and a single decimal point`() {
        assertEquals("123.45", AmountInput.sanitize("₹123.45"))
        assertEquals("12.34", AmountInput.sanitize("12.3.4"))
        assertEquals("450", AmountInput.sanitize("4a5b0"))
    }

    @Test
    fun `parse strips currency and grouping`() {
        assertEquals(BigDecimal("1234.00"), AmountInput.parse("₹1,234"))
    }

    @Test
    fun `parse rounds to paise`() {
        assertEquals(BigDecimal("12.35"), AmountInput.parse("12.345"))
    }

    @Test
    fun `parse rejects blank, zero and negatives`() {
        assertNull(AmountInput.parse(""))
        assertNull(AmountInput.parse("   "))
        assertNull(AmountInput.parse("0"))
        assertNull(AmountInput.parse("0.00"))
        assertNull(AmountInput.parse("-5"))
        assertNull(AmountInput.parse("abc"))
    }
}
