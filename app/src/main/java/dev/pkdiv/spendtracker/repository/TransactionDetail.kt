package dev.pkdiv.spendtracker.repository

import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant

enum class MessageUnavailableReason {
    DELETED,
    PERMISSION_REVOKED,
}

/** UI-ready detail combining parsed fields with the original source message. */
data class TransactionDetail(
    val id: Long,
    val amount: BigDecimal,
    val direction: TransactionDirection,
    val merchant: String,
    val account: String?,
    val category: TransactionCategory,
    val timestamp: Instant,
    val reference: String,
    val source: TransactionSource,
    val sender: String?,
    val subject: String?,
    val receivedAt: Instant,
    val body: String?,
    val bodyTruncated: Boolean,
)
