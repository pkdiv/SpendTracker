package dev.pkdiv.spendtracker.ui.history

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionCategorizer
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.TransactionRepository
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HistoryViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun openRepository(): Pair<SpendTrackerDatabase, TransactionRepository> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = TransactionRepository(
            db.transactionDao(),
            db.unrecognizedMessageDao(),
            db.merchantCategoryDao(),
            TransactionCategorizer(),
        )
        return db to repository
    }

    private var counter = 0

    private fun row(
        merchant: String,
        direction: TransactionDirection = TransactionDirection.DEBIT,
        category: TransactionCategory = TransactionCategory.FOOD,
        isDuplicate: Boolean = false,
    ) = TransactionEntity(
        amount = BigDecimal("100.00"),
        direction = direction,
        merchant = merchant,
        account = null,
        category = category,
        timestamp = Instant.ofEpochMilli(1700000000000),
        rawMessageRef = "ref:${counter++}",
        isDuplicate = isDuplicate,
    )

    @Test
    fun setDirection_keepsOnlyMatchingDirection() = runBlocking {
        val (db, repository) = openRepository()
        db.transactionDao().insertAll(listOf(row("Swiggy"), row("Salary", direction = TransactionDirection.CREDIT)))
        val viewModel = HistoryViewModel(repository)

        viewModel.setDirection(TransactionDirection.CREDIT)

        val state = viewModel.uiState.first {
            it.filters.direction == TransactionDirection.CREDIT && it.transactions.size == 1
        }
        assertEquals("Salary", state.transactions.single().merchant)
        db.close()
    }

    @Test
    fun setExcludeDuplicates_hidesDuplicateRows() = runBlocking {
        val (db, repository) = openRepository()
        db.transactionDao().insertAll(listOf(row("Swiggy"), row("Swiggy", isDuplicate = true)))
        val viewModel = HistoryViewModel(repository)

        viewModel.setExcludeDuplicates(true)

        val state = viewModel.uiState.first {
            it.filters.excludeDuplicates && it.transactions.size == 1
        }
        assertEquals(false, state.transactions.single().isDuplicate)
        db.close()
    }

    @Test
    fun clearFilters_resetsCriteriaAndRestoresAllRows() = runBlocking {
        val (db, repository) = openRepository()
        db.transactionDao().insertAll(
            listOf(row("Swiggy"), row("Salary", direction = TransactionDirection.CREDIT)),
        )
        val viewModel = HistoryViewModel(repository)
        viewModel.setDirection(TransactionDirection.CREDIT)
        viewModel.uiState.first { it.filters.direction == TransactionDirection.CREDIT && it.transactions.size == 1 }

        viewModel.clearFilters()

        val state = viewModel.uiState.first {
            it.filters == HistoryFilters() && it.transactions.size == 2
        }
        assertEquals(2, state.transactions.size)
        db.close()
    }

    @Test
    fun setCategory_filtersByCategory() = runBlocking {
        val (db, repository) = openRepository()
        db.transactionDao().insertAll(
            listOf(row("Swiggy", category = TransactionCategory.FOOD), row("Uber", category = TransactionCategory.TRANSPORT)),
        )
        val viewModel = HistoryViewModel(repository)

        viewModel.setCategory(TransactionCategory.TRANSPORT)

        val state = viewModel.uiState.first {
            it.filters.category == TransactionCategory.TRANSPORT && it.transactions.size == 1
        }
        assertEquals("Uber", state.transactions.single().merchant)
        db.close()
    }
}
