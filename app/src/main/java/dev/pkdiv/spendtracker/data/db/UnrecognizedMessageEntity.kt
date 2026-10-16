package dev.pkdiv.spendtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "unrecognized_messages",
    indices = [Index(value = ["rawMessageRef"], unique = true)],
)
data class UnrecognizedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val rawMessageRef: String,
    val receivedAt: Instant,
    val smsId: Long? = null,
    // Soft-delete marker: kept (never physically removed) so the rawMessageRef unique
    // index still blocks the SMS backfill from re-inserting a dismissed message.
    @ColumnInfo(defaultValue = "0")
    val dismissed: Boolean = false,
)
