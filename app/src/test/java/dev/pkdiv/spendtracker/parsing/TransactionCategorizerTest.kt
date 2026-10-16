package dev.pkdiv.spendtracker.parsing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TransactionCategorizerTest {

    private val categorizer = TransactionCategorizer()

    @Test
    fun `matches case-insensitively`() {
        assertEquals(TransactionCategory.FOOD, categorizer.categorize("swiggy"))
        assertEquals(TransactionCategory.FOOD, categorizer.categorize("ZOMATO"))
    }

    @Test
    fun `specific rules beat generic rules`() {
        assertEquals(TransactionCategory.ENTERTAINMENT, categorizer.categorize("Amazon Prime"))
        assertEquals(TransactionCategory.ENTERTAINMENT, categorizer.categorize("PRIME VIDEO"))
        assertEquals(TransactionCategory.FOOD, categorizer.categorize("Instamart"))
        assertEquals(TransactionCategory.FOOD, categorizer.categorize("BigBasket"))
    }

    @Test
    fun `categorizes common merchants`() {
        assertEquals(TransactionCategory.SHOPPING, categorizer.categorize("Amazon"))
        assertEquals(TransactionCategory.TRANSPORT, categorizer.categorize("Uber"))
        assertEquals(TransactionCategory.HEALTH, categorizer.categorize("Apollo Pharmacy"))
        assertEquals(TransactionCategory.BILLS, categorizer.categorize("Airtel Recharge"))
        assertEquals(TransactionCategory.SELF_TRANSFER, categorizer.categorize("Own Account"))
    }

    @Test
    fun `unknown merchants fall back to other`() {
        assertEquals(TransactionCategory.OTHER, categorizer.categorize("Unknown"))
        assertEquals(TransactionCategory.OTHER, categorizer.categorize("Some Random Merchant"))
        assertEquals(TransactionCategory.OTHER, categorizer.categorize("   "))
    }
}
