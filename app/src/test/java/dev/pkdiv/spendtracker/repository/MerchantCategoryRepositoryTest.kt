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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the merchant -> category editor's data path: normalized writes, upsert/delete
 * semantics, and re-classifying a transaction through the repository.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MerchantCategoryRepositoryTest {

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

    private fun row(merchant: String): TransactionEntity = TransactionEntity(
        amount = BigDecimal("100.00"),
        direction = TransactionDirection.DEBIT,
        merchant = merchant,
        account = null,
        category = TransactionCategory.OTHER,
        timestamp = Instant.ofEpochMilli(1700000000000),
        rawMessageRef = "ref:${counter++}",
        source = TransactionSource.SMS,
        receivedAt = 1700000000000,
    )

    private suspend fun categoryOf(id: Long): TransactionCategory? =
        db.transactionDao().observeById(id).first()?.category

    @Test
    fun setMerchantCategory_normalizesKey_andReclassifiesTransaction() = runBlocking {
        setUp()
        val id = db.transactionDao().insert(row("Swiggy"))

        repository.setMerchantCategory("  swiggy ", TransactionCategory.BILLS)

        assertEquals(
            listOf(MerchantCategoryMapping("SWIGGY", TransactionCategory.BILLS)),
            repository.observeMerchantCategories().first(),
        )
        assertEquals(1, repository.recategorizeOtherTransactions())
        assertEquals(TransactionCategory.BILLS, categoryOf(id))
        db.close()
    }

    @Test
    fun setMerchantCategory_upsertReplacesExistingCategory() = runBlocking {
        setUp()
        repository.setMerchantCategory("Swiggy", TransactionCategory.FOOD)

        repository.setMerchantCategory("swiggy", TransactionCategory.SHOPPING)

        assertEquals(
            listOf(MerchantCategoryMapping("SWIGGY", TransactionCategory.SHOPPING)),
            repository.observeMerchantCategories().first(),
        )
        db.close()
    }

    @Test
    fun deleteMerchantCategory_removesMapping() = runBlocking {
        setUp()
        repository.setMerchantCategory("Swiggy", TransactionCategory.FOOD)

        repository.deleteMerchantCategory("swiggy")

        assertEquals(emptyList<MerchantCategoryMapping>(), repository.observeMerchantCategories().first())
        db.close()
    }

    @Test
    fun setMerchantCategory_ignoresBlankMerchant() = runBlocking {
        setUp()

        repository.setMerchantCategory("   ", TransactionCategory.FOOD)

        assertEquals(emptyList<MerchantCategoryMapping>(), repository.observeMerchantCategories().first())
        db.close()
    }
}
