package dev.pkdiv.spendtracker.parsing

import java.math.BigDecimal
import java.time.Instant

data class ParsedTransaction(
    val amount: BigDecimal,
    val direction: TransactionDirection,
    val merchant: String,
    val account: String?,
    val timestamp: Instant?,
    val rawMessageRef: String,
    val category: TransactionCategory = TransactionCategory.OTHER,
)
