package dev.pkdiv.spendtracker.data.db

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives [SpendTrackerDatabase.MIGRATION_7_8] against a v7-shaped database and asserts
 * existing alerts survive and default to not-dismissed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class Migration7To8Test {

    private val v7TransactionSchema = """
        CREATE TABLE IF NOT EXISTS `transactions` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `amount` TEXT NOT NULL,
            `direction` TEXT NOT NULL,
            `merchant` TEXT NOT NULL,
            `account` TEXT,
            `category` TEXT NOT NULL,
            `timestamp` INTEGER NOT NULL,
            `rawMessageRef` TEXT NOT NULL,
            `smsId` INTEGER,
            `rawSender` TEXT,
            `rawBody` TEXT,
            `source` TEXT,
            `sourceRef` TEXT,
            `rawSubject` TEXT,
            `receivedAt` INTEGER,
            `bodyTruncated` INTEGER,
            `isDuplicate` INTEGER NOT NULL DEFAULT 0
        )
    """.trimIndent()

    private val v7UnrecognizedSchema = """
        CREATE TABLE IF NOT EXISTS `unrecognized_messages` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `sender` TEXT NOT NULL,
            `body` TEXT NOT NULL,
            `rawMessageRef` TEXT NOT NULL,
            `receivedAt` INTEGER NOT NULL,
            `smsId` INTEGER
        )
    """.trimIndent()

    private val v7MerchantCategorySchema = """
        CREATE TABLE IF NOT EXISTS `merchant_categories` (
            `merchant` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            PRIMARY KEY(`merchant`)
        )
    """.trimIndent()

    private fun createV7Indexes(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_rawMessageRef` ON `transactions` (`rawMessageRef`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_receivedAt` ON `transactions` (`receivedAt`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_unrecognized_messages_rawMessageRef` ON `unrecognized_messages` (`rawMessageRef`)")
    }

    private fun openInMemoryDatabase(context: Context): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(callback)
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase
    }

    @Test
    fun migrate7To8_preservesAlertsAndDefaultsToNotDismissed() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openInMemoryDatabase(context)
        try {
            db.execSQL(v7UnrecognizedSchema)
            db.execSQL(
                "INSERT INTO `unrecognized_messages` " +
                    "(`sender`,`body`,`rawMessageRef`,`receivedAt`,`smsId`) " +
                    "VALUES ('HDFCBK','Rs.100 debited','sms:7',1700000000000,7)",
            )

            SpendTrackerDatabase.MIGRATION_7_8.migrate(db)

            db.query(
                "SELECT sender, body, rawMessageRef, receivedAt, smsId, dismissed FROM unrecognized_messages",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("HDFCBK", cursor.getString(0))
                assertEquals("Rs.100 debited", cursor.getString(1))
                assertEquals("sms:7", cursor.getString(2))
                assertEquals(1700000000000L, cursor.getLong(3))
                assertEquals(7L, cursor.getLong(4))
                assertEquals(0L, cursor.getLong(5))
                assertFalse("no rows should be lost or duplicated", cursor.moveToNext())
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate7To8_passesRoomSchemaValidation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-7-8-test.db"
        context.deleteDatabase(dbName)

        val callback = object : SupportSQLiteOpenHelper.Callback(7) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase.use { db ->
            db.execSQL(v7TransactionSchema)
            db.execSQL(v7UnrecognizedSchema)
            db.execSQL(v7MerchantCategorySchema)
            createV7Indexes(db)
            db.execSQL(
                "INSERT INTO `unrecognized_messages` " +
                    "(`sender`,`body`,`rawMessageRef`,`receivedAt`,`smsId`) " +
                    "VALUES ('HDFCBK','Rs.100 debited','sms:7',1700000000000,7)",
            )
        }

        val room = Room.databaseBuilder(context, SpendTrackerDatabase::class.java, dbName)
            .addMigrations(SpendTrackerDatabase.MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()

        try {
            val rows = room.unrecognizedMessageDao().observeAll().first()
            assertEquals(1, rows.size)
            assertEquals("sms:7", rows.first().rawMessageRef)
            assertFalse(rows.first().dismissed)
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }
}
