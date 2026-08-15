package dev.pkdiv.spendtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun observeBetween(from: Instant, to: Instant): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    suspend fun between(from: Instant, to: Instant): List<TransactionEntity>

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: TransactionCategory)
}
