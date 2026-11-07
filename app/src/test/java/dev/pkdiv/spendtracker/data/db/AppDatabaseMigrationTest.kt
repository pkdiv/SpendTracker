package dev.pkdiv.spendtracker.data.db

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.di.AppModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Opens the database exactly as the app builds it ([AppModule.provideDatabase]) over an
 * existing older-schema database. Guards against a migration that is defined but never
 * registered in the Hilt provider — which crashes the app on upgrade even though the
 * per-migration tests pass (each of those registers its migrations explicitly).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppDatabaseMigrationTest {

    private val dbName = "spendtracker.db"

    private val v8TransactionSchema = """
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

    private val v8UnrecognizedSchema = """
        CREATE TABLE IF NOT EXISTS `unrecognized_messages` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `sender` TEXT NOT NULL,
            `body` TEXT NOT NULL,
            `rawMessageRef` TEXT NOT NULL,
            `receivedAt` INTEGER NOT NULL,
            `smsId` INTEGER,
            `dismissed` INTEGER NOT NULL DEFAULT 0
        )
    """.trimIndent()

    private val v8MerchantCategorySchema = """
        CREATE TABLE IF NOT EXISTS `merchant_categories` (
            `merchant` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            PRIMARY KEY(`merchant`)
        )
    """.trimIndent()

    @Test
    fun appDatabase_migratesOldInstallToLatest_preservingRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        val callback = object : SupportSQLiteOpenHelper.Callback(8) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase.use { db ->
            db.execSQL(v8TransactionSchema)
            db.execSQL(v8UnrecognizedSchema)
            db.execSQL(v8MerchantCategorySchema)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_rawMessageRef` ON `transactions` (`rawMessageRef`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_receivedAt` ON `transactions` (`receivedAt`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_unrecognized_messages_rawMessageRef` ON `unrecognized_messages` (`rawMessageRef`)")
            db.execSQL(
                "INSERT INTO `transactions` " +
                    "(`amount`,`direction`,`merchant`,`account`,`category`,`timestamp`,`rawMessageRef`,`receivedAt`,`isDuplicate`) " +
                    "VALUES ('100.00','DEBIT','Swiggy','1234','FOOD',1700000000000,'sms:7',1700000000000,0)",
            )
        }

        val room = AppModule.provideDatabase(context)
        try {
            val rows = room.transactionDao().observeAll().first()
            assertEquals(1, rows.size)
            assertEquals("Swiggy", rows.first().merchant)
            assertNull(rows.first().receiptUri)
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }
}
