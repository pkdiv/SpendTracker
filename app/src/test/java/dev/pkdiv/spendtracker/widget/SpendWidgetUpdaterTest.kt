package dev.pkdiv.spendtracker.widget

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionCategorizer
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.TransactionRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SpendWidgetUpdaterTest {

    private var counter = 0

    private fun row(amount: String, at: Instant, isDuplicate: Boolean = false) = TransactionEntity(
        amount = BigDecimal(amount),
        direction = TransactionDirection.DEBIT,
        merchant = "Merchant",
        account = null,
        category = TransactionCategory.FOOD,
        timestamp = at,
        rawMessageRef = "ref:${counter++}",
        source = TransactionSource.SMS,
        receivedAt = at.toEpochMilli(),
        isDuplicate = isDuplicate,
    )

    @Test
    fun monthToDate_readsOnlyTheCurrentMonth() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = TransactionRepository(
            transactionDao = db.transactionDao(),
            unrecognizedMessageDao = db.unrecognizedMessageDao(),
            merchantCategoryDao = db.merchantCategoryDao(),
            categorizer = TransactionCategorizer(),
        )
        val updater = SpendWidgetUpdater(context, repository)

        val zone = ZoneId.systemDefault()
        val inMonth = LocalDate.of(2026, 10, 14).atStartOfDay(zone).toInstant()
        val previousMonth = LocalDate.of(2026, 9, 20).atStartOfDay(zone).toInstant()
        db.transactionDao().insert(row("120.00", inMonth))
        db.transactionDao().insert(row("30.00", inMonth))
        db.transactionDao().insert(row("200.00", previousMonth))
        db.transactionDao().insert(row("75.00", inMonth, isDuplicate = true))

        val spend = updater.currentSpend(LocalDate.of(2026, 10, 18))

        assertEquals(BigDecimal("150.00"), spend.totalSpend)
        assertEquals(2, spend.transactionCount)
        db.close()
    }
}
