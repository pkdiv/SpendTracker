package dev.pkdiv.spendtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnrecognizedMessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: UnrecognizedMessageEntity): Long

    @Query("SELECT * FROM unrecognized_messages WHERE dismissed = 0 ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<UnrecognizedMessageEntity>>

    /** Soft-dismiss: the row is kept so the backfill can't re-insert the same message. */
    @Query("UPDATE unrecognized_messages SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("UPDATE unrecognized_messages SET dismissed = 1")
    suspend fun dismissAll()

    /**
     * Whether a message with this content is already known (dismissed or not). Guards
     * against the live receiver and the backfill reader producing different
     * rawMessageRefs for the same physical SMS.
     */
    @Query("SELECT COUNT(*) FROM unrecognized_messages WHERE sender = :sender AND receivedAt = :receivedAt AND body = :body")
    suspend fun countForContent(sender: String, receivedAt: Long, body: String): Int
}
