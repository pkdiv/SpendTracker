package dev.pkdiv.spendtracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant

@Database(
    entities = [
        TransactionEntity::class,
        UnrecognizedMessageEntity::class,
        MerchantCategoryEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SpendTrackerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun unrecognizedMessageDao(): UnrecognizedMessageDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM transactions WHERE id NOT IN (SELECT MIN(id) FROM transactions GROUP BY rawMessageRef)")
                db.execSQL("DELETE FROM unrecognized_messages WHERE id NOT IN (SELECT MIN(id) FROM unrecognized_messages GROUP BY rawMessageRef)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_rawMessageRef ON transactions (rawMessageRef)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_unrecognized_messages_rawMessageRef ON unrecognized_messages (rawMessageRef)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsId INTEGER")
                db.execSQL("ALTER TABLE unrecognized_messages ADD COLUMN smsId INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN rawSender TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN rawBody TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM transactions WHERE rawSender IS NULL OR rawBody IS NULL")
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal): String = value.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal = BigDecimal(value)

    @TypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun fromDirection(value: TransactionDirection): String = value.name

    @TypeConverter
    fun toDirection(value: String): TransactionDirection = TransactionDirection.valueOf(value)

    @TypeConverter
    fun fromCategory(value: TransactionCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): TransactionCategory = TransactionCategory.valueOf(value)
}
