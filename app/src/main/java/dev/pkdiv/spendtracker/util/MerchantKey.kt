package dev.pkdiv.spendtracker.util

/**
 * Canonical form of a merchant key used by the user-editable `merchant_categories`
 * table. Writes go through [normalize] so two spellings of the same merchant
 * ("Swiggy" / "swiggy ") collapse to a single row, and message-derived lookups
 * match those stored keys exactly.
 */
object MerchantKey {
    fun normalize(merchant: String): String = merchant.trim().uppercase()
}
