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
)
