package dev.pkdiv.spendtracker.ui.addtransaction

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManualTransactionFormTest {

    @Test
    fun `valid form has no errors and a paise-precision amount`() {
        val form = ManualTransactionForm(amount = "450", merchant = "Chai stall")

        assertEquals(ManualTransactionErrors(), form.errors())
        assertEquals(BigDecimal("450.00"), form.amountValue)
    }

    @Test
    fun `blank amount and merchant are errors`() {
        val errors = ManualTransactionForm(amount = "", merchant = "   ").errors()

        assertTrue(errors.amount)
        assertTrue(errors.merchant)
    }

    @Test
    fun `zero amount is an error`() {
        val errors = ManualTransactionForm(amount = "0", merchant = "Chai stall").errors()

        assertTrue(errors.amount)
        assertFalse(errors.merchant)
    }

    @Test
    fun `merchant is trimmed`() {
        assertEquals("Chai stall", ManualTransactionForm(merchant = "  Chai stall  ").merchantValue)
    }
}
