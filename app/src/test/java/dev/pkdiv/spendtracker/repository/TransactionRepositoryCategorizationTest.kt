package dev.pkdiv.spendtracker.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.db.MerchantCategoryEntity
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionCategorizer
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TransactionRepositoryCategorizationTest {

    private lateinit var db: SpendTrackerDatabase
    private lateinit var repository: TransactionRepository

    private fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransactionRepository(
            transactionDao = db.transactionDao(),
            unrecognizedMessageDao = db.unrecognizedMessageDao(),
            merchantCategoryDao = db.merchantCategoryDao(),
            categorizer = TransactionCategorizer(),
        )
    }

    private var counter = 0

    private fun row(merchant: String, category: TransactionCategory): TransactionEntity = TransactionEntity(
        amount = BigDecimal("100.00"),
        direction = TransactionDirection.DEBIT,
        merchant = merchant,
        account = null,
        category = category,
        timestamp = Instant.ofEpochMilli(1700000000000),
        rawMessageRef = "ref:${counter++}",
        source = TransactionSource.SMS,
        receivedAt = 1700000000000,
    )

    private suspend fun categoryOf(id: Long): TransactionCategory? =
        db.transactionDao().observeById(id).first()?.category

    @Test
    fun reclassifiesOtherRows_leavesUnresolvableAsOther() = runBlocking {
        setUp()
        val resolvedId = db.transactionDao().insert(row("Swiggy", TransactionCategory.OTHER))
        val unknownId = db.transactionDao().insert(row("Zqr Unmapped Vendor", TransactionCategory.OTHER))

        val updated = repository.recategorizeOtherTransactions()

        assertEquals(1, updated)
        assertEquals(TransactionCategory.FOOD, categoryOf(resolvedId))
        assertEquals(TransactionCategory.OTHER, categoryOf(unknownId))
        db.close()
    }

    @Test
    fun leavesNonOtherRowsAlone_evenWhenAKnownMerchant() = runBlocking {
        setUp()
        // Simulates a row the user manually re-categorized away from OTHER.
        val id = db.transactionDao().insert(row("Swiggy", TransactionCategory.TRANSPORT))

        val updated = repository.recategorizeOtherTransactions()

        assertEquals(0, updated)
        assertEquals(TransactionCategory.TRANSPORT, categoryOf(id))
        db.close()
    }

    @Test
    fun userOverrideWinsOverBuiltInDefaults() = runBlocking {
        setUp()
        db.merchantCategoryDao().upsert(
            MerchantCategoryEntity(merchant = "Zqr Unmapped Vendor", category = TransactionCategory.BILLS.name),
        )
        val id = db.transactionDao().insert(row("Zqr Unmapped Vendor", TransactionCategory.OTHER))

        repository.recategorizeOtherTransactions()

        assertEquals(TransactionCategory.BILLS, categoryOf(id))
        db.close()
    }

    @Test
    fun isIdempotentAndResumesAcrossBatches() = runBlocking {
        setUp()
        val ids = listOf("Swiggy", "Uber", "Netflix").map {
            db.transactionDao().insert(row(it, TransactionCategory.OTHER))
        }

        val first = repository.recategorizeOtherTransactions(batchSize = 1)
        val second = repository.recategorizeOtherTransactions(batchSize = 1)

        assertEquals(3, first)
        assertEquals(0, second)
        assertEquals(TransactionCategory.FOOD, categoryOf(ids[0]))
        assertEquals(TransactionCategory.TRANSPORT, categoryOf(ids[1]))
        assertEquals(TransactionCategory.ENTERTAINMENT, categoryOf(ids[2]))
        db.close()
    }
}
