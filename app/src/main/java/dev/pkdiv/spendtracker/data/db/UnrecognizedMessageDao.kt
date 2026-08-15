package dev.pkdiv.spendtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnrecognizedMessageDao {

    @Insert
    suspend fun insert(message: UnrecognizedMessageEntity): Long

    @Query("SELECT * FROM unrecognized_messages ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<UnrecognizedMessageEntity>>

    @Query("DELETE FROM unrecognized_messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM unrecognized_messages")
    suspend fun clear()
}
