package dev.pkdiv.spendtracker.repository

import dev.pkdiv.spendtracker.parsing.TransactionCategory

/**
 * A user-taught merchant -> category override, ready for display and editing.
 * [merchant] is the normalized key stored in `merchant_categories`.
 */
data class MerchantCategoryMapping(
    val merchant: String,
    val category: TransactionCategory,
)
