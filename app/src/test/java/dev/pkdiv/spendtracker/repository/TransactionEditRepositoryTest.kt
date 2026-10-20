package dev.pkdiv.spendtracker.repository

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
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the notes + rename-merchant data path: the merchant is updated on the row, the
 * rename is fed into the merchant_categories mapping (normalized), and notes are trimmed
 * with blank clearing to null.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TransactionEditRepositoryTest {

    private lateinit var db: SpendTrackerDatabase
    private lateinit var repository: TransactionRepository
    private var counter = 0

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

    private fun row(
        merchant: String = "Swiggy",
        category: TransactionCategory = TransactionCategory.FOOD,
    ): TransactionEntity = TransactionEntity(
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

    private suspend fun entityOf(id: Long): TransactionEntity? =
        db.transactionDao().observeById(id).first()

    @Test
    fun renameMerchant_updatesMerchant_andTeachesNormalizedMapping() = runBlocking {
        setUp()
        val id = db.transactionDao().insert(row(merchant = "UPI/1234/SWIGGY", category = TransactionCategory.FOOD))

        repository.renameMerchant(id, "  Swiggy  ")

        assertEquals("Swiggy", entityOf(id)?.merchant)
        assertEquals(
            listOf(MerchantCategoryMapping("SWIGGY", TransactionCategory.FOOD)),
            repository.observeMerchantCategories().first(),
        )
        db.close()
    }

    @Test
    fun renameMerchant_ignoresBlankName() = runBlocking {
        setUp()
        val id = db.transactionDao().insert(row(merchant = "Swiggy"))

        repository.renameMerchant(id, "   ")

        assertEquals("Swiggy", entityOf(id)?.merchant)
        assertEquals(emptyList<MerchantCategoryMapping>(), repository.observeMerchantCategories().first())
        db.close()
    }

    @Test
    fun setNote_storesTrimmedNote() = runBlocking {
        setUp()
        val id = db.transactionDao().insert(row())

        repository.setNote(id, "  Split with Ravi  ")

        assertEquals("Split with Ravi", entityOf(id)?.note)
        db.close()
    }

    @Test
    fun setNote_clearsBlankToNull() = runBlocking {
        setUp()
        val id = db.transactionDao().insert(row())

        repository.setNote(id, "First")
        repository.setNote(id, "   ")

        assertNull(entityOf(id)?.note)
        db.close()
    }
}
