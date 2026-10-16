package dev.pkdiv.spendtracker.ui.unrecognized

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategorizer
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.testutil.ViewModelTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class UnrecognizedViewModelTest : ViewModelTest() {

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

    @Test
    fun clearAll_removesEveryAlert() = runBlocking {
        val (db, repository) = openRepository()
        repository.insertUnrecognized("HDFCBK", "Rs.100 debited", "ref:1", 1700000000000)
        repository.insertUnrecognized("ICICIB", "Rs.200 debited", "ref:2", 1700000001000)
        val viewModel = track(UnrecognizedViewModel(repository))
        viewModel.uiState.first { it.messages.size == 2 }

        viewModel.clearAll()

        val state = viewModel.uiState.first { it.messages.isEmpty() }
        assertEquals(emptyList<UnrecognizedMessageEntity>(), state.messages)
        db.close()
    }

    @Test
    fun dismiss_removesOnlyThatAlert() = runBlocking {
        val (db, repository) = openRepository()
        repository.insertUnrecognized("HDFCBK", "Rs.100 debited", "ref:1", 1700000000000)
        repository.insertUnrecognized("ICICIB", "Rs.200 debited", "ref:2", 1700000001000)
        val viewModel = track(UnrecognizedViewModel(repository))
        val initial = viewModel.uiState.first { it.messages.size == 2 }
        val target = initial.messages.first { it.rawMessageRef == "ref:1" }

        viewModel.dismiss(target.id)

        val state = viewModel.uiState.first { it.messages.size == 1 }
        assertEquals("ref:2", state.messages.single().rawMessageRef)
        db.close()
    }
}
