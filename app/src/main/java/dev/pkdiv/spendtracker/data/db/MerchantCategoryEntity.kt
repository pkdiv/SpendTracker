package dev.pkdiv.spendtracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_categories")
data class MerchantCategoryEntity(
    @PrimaryKey val merchant: String,
    val category: String,
)
