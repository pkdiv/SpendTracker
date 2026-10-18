package dev.pkdiv.spendtracker.ui.addtransaction

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionCategorizer
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.testutil.ViewModelTest
import dev.pkdiv.spendtracker.widget.WidgetRefresher
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AddTransactionViewModelTest : ViewModelTest() {

    private class FakeRefresher : WidgetRefresher {
        var count = 0
        override fun refresh() {
            count++
        }
    }

    private lateinit var db: SpendTrackerDatabase
    private lateinit var repository: TransactionRepository

    private fun openRepository(): FakeRefresher {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransactionRepository(
            db.transactionDao(),
            db.unrecognizedMessageDao(),
            db.merchantCategoryDao(),
            TransactionCategorizer(),
        )
        return FakeRefresher()
    }

    private fun viewModel(refresher: WidgetRefresher) =
        track(AddTransactionViewModel(repository, refresher))

    @Test
    fun saveStoresManualTransaction_andSignalsSaved() = runBlocking {
        val refresher = openRepository()
        val viewModel = viewModel(refresher)
        viewModel.setAmount("450")
        viewModel.setMerchant("  Chai stall  ")
        viewModel.setCategory(TransactionCategory.FOOD)
        viewModel.setAccount("1234")
        viewModel.setDate(LocalDate.of(2026, 10, 12))

        viewModel.save()
        viewModel.uiState.first { it is AddTransactionUiState.Saved }

        val row = repository.observeAll().first().single()
        assertEquals(BigDecimal("450.00"), row.amount)
        assertEquals(TransactionDirection.DEBIT, row.direction)
        assertEquals("Chai stall", row.merchant)
        assertEquals(TransactionCategory.FOOD, row.category)
        assertEquals("1234", row.account)
        assertEquals(TransactionSource.MANUAL, row.source)
        assertTrue(row.rawMessageRef.startsWith("manual:"))
        assertFalse(row.isDuplicate)
        assertEquals(
            LocalDate.of(2026, 10, 12).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            row.timestamp,
        )
        assertEquals(1, refresher.count)
        db.close()
    }

    @Test
    fun chosenCategory_winsOverTheCategorizer() = runBlocking {
        val refresher = openRepository()
        val viewModel = viewModel(refresher)
        viewModel.setAmount("182.50")
        viewModel.setMerchant("Uber")
        // The keyword categorizer would place Uber under TRANSPORT; the user's pick wins.
        viewModel.setCategory(TransactionCategory.FOOD)

        viewModel.save()
        viewModel.uiState.first { it is AddTransactionUiState.Saved }

        assertEquals(TransactionCategory.FOOD, repository.observeAll().first().single().category)
        db.close()
    }

    @Test
    fun blankAmount_doesNotStore_andShowsError() = runBlocking {
        val refresher = openRepository()
        val viewModel = viewModel(refresher)
        viewModel.setMerchant("Chai stall")

        viewModel.save()

        val state = viewModel.uiState.value as AddTransactionUiState.Editing
        assertTrue(state.errors.amount)
        assertTrue(repository.observeAll().first().isEmpty())
        assertEquals(0, refresher.count)
        db.close()
    }

    @Test
    fun blankMerchant_doesNotStore_andShowsError() = runBlocking {
        val refresher = openRepository()
        val viewModel = viewModel(refresher)
        viewModel.setAmount("100")

        viewModel.save()

        val state = viewModel.uiState.value as AddTransactionUiState.Editing
        assertTrue(state.errors.merchant)
        assertTrue(repository.observeAll().first().isEmpty())
        db.close()
    }
}
