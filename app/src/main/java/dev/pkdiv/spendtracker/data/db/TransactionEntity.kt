package dev.pkdiv.spendtracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["rawMessageRef"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: BigDecimal,
    val direction: TransactionDirection,
    val merchant: String,
    val account: String?,
    val category: TransactionCategory,
    val timestamp: Instant,
    val rawMessageRef: String,
    val smsId: Long? = null,
    val rawSender: String? = null,
    val rawBody: String? = null,
    // Added in schema v6. Legacy rows carry nulls until the backfill/normalization runs.
    val source: TransactionSource? = null,
    val sourceRef: String? = null,
    val rawSubject: String? = null,
    val receivedAt: Long? = null,
    val bodyTruncated: Boolean? = null,
)
