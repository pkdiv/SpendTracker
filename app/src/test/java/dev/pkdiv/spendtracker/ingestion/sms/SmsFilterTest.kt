package dev.pkdiv.spendtracker.ingestion.sms

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmsFilterTest {

    @Test
    fun `accepts bank debit and credit alerts`() {
        assertTrue(
            SmsFilter.isTransactionShaped(
                "HDFC-BANK",
                "Dear Customer, Rs 1,250.00 debited from your HDFC Bank account xx1234 at AMAZON on 12-08-2026.",
            ),
        )
        assertTrue(
            SmsFilter.isTransactionShaped(
                "SBIINB",
                "Your A/c XX1234 has been credited by Rs.5,000.00 on 12-08-2026.",
            ),
        )
    }

    @Test
    fun `accepts UPI and card payment alerts`() {
        assertTrue(
            SmsFilter.isTransactionShaped(
                "UPI",
                "UPI txn of Rs 250.00 to merchant@upi on 12-08-2026 debited from your account.",
            ),
        )
        assertTrue(
            SmsFilter.isTransactionShaped(
                "ICICIB",
                "INR 1,499.00 spent on your ICICI Bank Credit Card ending 5678 at FLIPKART.",
            ),
        )
    }

    @Test
    fun `rejects personal messages`() {
        assertFalse(SmsFilter.isTransactionShaped("+919876543210", "Hey, are we still meeting at 7?"))
        assertFalse(SmsFilter.isTransactionShaped("AMAZON", "Your order has been shipped and will arrive tomorrow."))
    }

    @Test
    fun `rejects OTP and verification messages even when they mention an amount`() {
        assertFalse(SmsFilter.isTransactionShaped("HDFCBK", "Your OTP is 123456."))
        assertFalse(
            SmsFilter.isTransactionShaped(
                "HDFCBK",
                "123456 is your OTP for a transaction of Rs 500.00. Do not share it with anyone.",
            ),
        )
        assertFalse(
            SmsFilter.isTransactionShaped(
                "ICICIB",
                "Your verification code is 987654 for a payment of INR 1,000.00.",
            ),
        )
    }

    @Test
    fun `rejects marketing and balance-only messages`() {
        assertFalse(
            SmsFilter.isTransactionShaped(
                "HDFC-BANK",
                "Get up to Rs 5,000 cashback on your credit card. Offer ends soon.",
            ),
        )
        assertFalse(
            SmsFilter.isTransactionShaped("SBIINB", "Your account balance is Rs 5,000.00 as on 12-08-2026."),
        )
    }
}
