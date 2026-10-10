package dev.pkdiv.spendtracker.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrivacyTest {

    @Test
    fun maskSensitive_masksOtpCodes() {
        val input = "Your OTP is 123456. Verification code: 9876. code#888888"
        val masked = Privacy.maskSensitive(input)
        assertFalse(masked.contains("123456"))
        assertFalse(masked.contains("9876"))
        assertFalse(masked.contains("888888"))
        assertTrue(masked.contains("••••"))
    }

    @Test
    fun maskSensitive_leavesPlainTextUntouched() {
        val input = "Rs.100 spent at Swiggy on card 1234"
        assertEquals(input, Privacy.maskSensitive(input))
    }

    @Test
    fun capBody_truncatesAndFlags() {
        val truncated = Privacy.capBody("abc", limit = 2)
        assertEquals("ab", truncated.text)
        assertTrue(truncated.truncated)

        val exact = Privacy.capBody("ab", limit = 2)
        assertEquals("ab", exact.text)
        assertFalse(exact.truncated)
    }
}
