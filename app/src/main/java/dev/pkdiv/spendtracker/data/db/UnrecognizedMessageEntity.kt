package dev.pkdiv.spendtracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "unrecognized_messages")
data class UnrecognizedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val rawMessageRef: String,
    val receivedAt: Instant,
)
