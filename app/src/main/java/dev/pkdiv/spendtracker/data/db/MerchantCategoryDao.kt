package dev.pkdiv.spendtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: MerchantCategoryEntity)

    @Query("SELECT category FROM merchant_categories WHERE merchant = :merchant")
    suspend fun categoryFor(merchant: String): String?

    @Query("SELECT * FROM merchant_categories ORDER BY merchant")
    fun observeAll(): Flow<List<MerchantCategoryEntity>>
}
