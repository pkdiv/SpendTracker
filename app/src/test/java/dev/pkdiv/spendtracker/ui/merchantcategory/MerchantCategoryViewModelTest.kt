package dev.pkdiv.spendtracker.ui.merchantcategory

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionCategorizer
import dev.pkdiv.spendtracker.repository.MerchantCategoryMapping
import dev.pkdiv.spendtracker.repository.TransactionRepository
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
class MerchantCategoryViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repository(): TransactionRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        return TransactionRepository(
            db.transactionDao(),
            db.unrecognizedMessageDao(),
            db.merchantCategoryDao(),
            TransactionCategorizer(),
        )
    }

    @Test
    fun content_whenMappingExists() = runBlocking {
        val repository = repository()
        repository.setMerchantCategory("Swiggy", TransactionCategory.FOOD)

        val state = MerchantCategoryViewModel(repository).uiState
            .first { it is MerchantCategoryUiState.Content && it.mappings.isNotEmpty() }

        assertEquals(
            MerchantCategoryUiState.Content(listOf(MerchantCategoryMapping("SWIGGY", TransactionCategory.FOOD))),
            state,
        )
    }

    @Test
    fun save_addsNormalizedMappingToState() = runBlocking {
        val viewModel = MerchantCategoryViewModel(repository())

        viewModel.save(original = null, merchant = "Swiggy", category = TransactionCategory.FOOD)

        val state = viewModel.uiState
            .first { it is MerchantCategoryUiState.Content && it.mappings.isNotEmpty() } as MerchantCategoryUiState.Content
        assertEquals(MerchantCategoryMapping("SWIGGY", TransactionCategory.FOOD), state.mappings.single())
    }

    @Test
    fun save_renamesKeyReplacingOldMapping() = runBlocking {
        val viewModel = MerchantCategoryViewModel(repository())
        viewModel.save(original = null, merchant = "Swiggy", category = TransactionCategory.FOOD)
        viewModel.uiState.first { it is MerchantCategoryUiState.Content && it.mappings.isNotEmpty() }

        viewModel.save(original = "SWIGGY", merchant = "Swiggy Bangalore", category = TransactionCategory.FOOD)

        val state = viewModel.uiState
            .first {
                it is MerchantCategoryUiState.Content &&
                    it.mappings.singleOrNull()?.merchant == "SWIGGY BANGALORE"
            } as MerchantCategoryUiState.Content
        assertEquals(MerchantCategoryMapping("SWIGGY BANGALORE", TransactionCategory.FOOD), state.mappings.single())
    }

    @Test
    fun delete_removesMappingFromState() = runBlocking {
        val repository = repository()
        repository.setMerchantCategory("Swiggy", TransactionCategory.FOOD)
        val viewModel = MerchantCategoryViewModel(repository)
        viewModel.uiState.first { it is MerchantCategoryUiState.Content && it.mappings.isNotEmpty() }

        viewModel.delete("SWIGGY")

        val state = viewModel.uiState.first { it is MerchantCategoryUiState.Content && it.mappings.isEmpty() }
        assertEquals(MerchantCategoryUiState.Content(emptyList()), state)
    }
}
