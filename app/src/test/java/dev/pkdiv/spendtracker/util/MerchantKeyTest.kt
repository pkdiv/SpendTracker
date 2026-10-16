package dev.pkdiv.spendtracker.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MerchantKeyTest {

    @Test
    fun `trims surrounding whitespace and uppercases`() {
        assertEquals("SWIGGY", MerchantKey.normalize("  Swiggy "))
    }

    @Test
    fun `collapses differing spellings to one key`() {
        assertEquals(MerchantKey.normalize("swiggy"), MerchantKey.normalize("SWIGGY"))
    }

    @Test
    fun `blank input stays empty`() {
        assertEquals("", MerchantKey.normalize("   "))
    }
}
