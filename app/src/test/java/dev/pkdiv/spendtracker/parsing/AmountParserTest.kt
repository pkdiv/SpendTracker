package dev.pkdiv.spendtracker.parsing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AmountParserTest {

    @Test
    fun `direction is credit for credited messages`() {
        assertEquals(
            TransactionDirection.CREDIT,
            AmountParser.direction("Rs 5,000.00 credited to your HDFC Bank account xx1234."),
        )
    }

    @Test
    fun `direction is credit for refunds`() {
        assertEquals(
            TransactionDirection.CREDIT,
            AmountParser.direction("Rs 1,000.00 refund has been added to your account."),
        )
    }

    @Test
    fun `direction is debit for credit card spends`() {
        assertEquals(
            TransactionDirection.DEBIT,
            AmountParser.direction("Rs 1,250.00 spent on your HDFC Bank Credit Card at AMAZON."),
        )
    }

    @Test
    fun `direction is debit for credit card debits`() {
        assertEquals(
            TransactionDirection.DEBIT,
            AmountParser.direction("Rs 4,500.00 debited from your HDFC Bank Credit Card xx1234."),
        )
    }

    @Test
    fun `direction is debit for credit card payment reminders`() {
        assertEquals(
            TransactionDirection.DEBIT,
            AmountParser.direction(
                "Reminder: Your HDFC Bank Credit Card payment of Rs 4,500.00 is due on 12-Nov-2026.",
            ),
        )
    }

    @Test
    fun `direction is debit for credit limit reminders`() {
        assertEquals(
            TransactionDirection.DEBIT,
            AmountParser.direction("Your HDFC Bank Credit Card credit limit is Rs 80,000.00."),
        )
    }
}
