package dev.pkdiv.spendtracker.data.db

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives [SpendTrackerDatabase.MIGRATION_6_7] against a v6-shaped database and asserts
 * existing rows survive and default to non-duplicate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class Migration6To7Test {

    private val v6Schema = """
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
            `bodyTruncated` INTEGER
        )
    """.trimIndent()

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
    fun migrate6To7_preservesRowsAndDefaultsToNonDuplicate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openInMemoryDatabase(context)
        try {
            db.execSQL(v6Schema)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_rawMessageRef` ON `transactions` (`rawMessageRef`)")
            db.execSQL(
                "INSERT INTO `transactions` " +
                    "(`amount`,`direction`,`merchant`,`account`,`category`,`timestamp`,`rawMessageRef`," +
                    "`smsId`,`rawSender`,`rawBody`,`source`,`sourceRef`,`rawSubject`,`receivedAt`,`bodyTruncated`) " +
                    "VALUES ('100.00','DEBIT','Swiggy',NULL,'FOOD',1700000000000,'sms:7',7,'HDFCBK'," +
                    "'Rs.100 debited from A/c','SMS','7',NULL,1700000000000,NULL)",
            )

            SpendTrackerDatabase.MIGRATION_6_7.migrate(db)

            db.query(
                "SELECT amount, receivedAt, isDuplicate, rawBody FROM transactions",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("100.00", cursor.getString(0))
                assertEquals(1700000000000L, cursor.getLong(1))
                assertEquals(0L, cursor.getLong(2))
                assertEquals("Rs.100 debited from A/c", cursor.getString(3))
                assertFalse("no rows should be lost or duplicated", cursor.moveToNext())
            }

            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_receivedAt'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("index_transactions_receivedAt", cursor.getString(0))
            }
        } finally {
            db.close()
        }
    }
}
