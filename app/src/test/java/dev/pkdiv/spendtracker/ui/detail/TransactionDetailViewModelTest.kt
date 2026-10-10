package dev.pkdiv.spendtracker.ui.detail

import android.Manifest
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.MessageUnavailableReason
import dev.pkdiv.spendtracker.repository.TransactionRepository
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.robolectric.shadows.ShadowApplication

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TransactionDetailViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun entity(rawBody: String?) = TransactionEntity(
        amount = BigDecimal("100.00"),
        direction = TransactionDirection.DEBIT,
        merchant = "Swiggy",
        account = "1234",
        category = TransactionCategory.FOOD,
        timestamp = Instant.ofEpochMilli(1700000000000),
        rawMessageRef = "sms:1",
        smsId = 1,
        rawSender = "HDFCBK",
        rawBody = rawBody,
        source = TransactionSource.SMS,
        receivedAt = 1700000000000,
    )

    private fun viewModel(id: Long, repository: TransactionRepository, context: Context) =
        TransactionDetailViewModel(SavedStateHandle(mapOf("id" to id)), repository, context)

    @Test
    fun content_whenBodyPresent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.transactionDao()
        val id = dao.insert(entity(rawBody = "Rs.100 debited"))
        val repository = TransactionRepository(dao, db.unrecognizedMessageDao())

        viewModel(id, repository, context).uiState.test {
            assertEquals(DetailUiState.Loading, awaitItem())
            val content = awaitItem() as DetailUiState.Content
            assertEquals("Rs.100 debited", content.detail.body)
        }
        db.close()
    }

    @Test
    fun notFound_whenIdMissing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = TransactionRepository(db.transactionDao(), db.unrecognizedMessageDao())

        viewModel(999L, repository, context).uiState.test {
            assertEquals(DetailUiState.Loading, awaitItem())
            assertEquals(DetailUiState.NotFound, awaitItem())
        }
        db.close()
    }

    @Test
    fun unavailable_permissionRevoked_whenBodyNullAndNoPermission() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.transactionDao()
        val id = dao.insert(entity(rawBody = null))
        val repository = TransactionRepository(dao, db.unrecognizedMessageDao())

        viewModel(id, repository, context).uiState.test {
            awaitItem()
            val unavailable = awaitItem() as DetailUiState.Unavailable
            assertEquals(MessageUnavailableReason.PERMISSION_REVOKED, unavailable.reason)
        }
        db.close()
    }

    @Test
    fun unavailable_deleted_whenBodyNullAndPermissionGranted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.READ_SMS)
        val db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.transactionDao()
        val id = dao.insert(entity(rawBody = null))
        val repository = TransactionRepository(dao, db.unrecognizedMessageDao())

        viewModel(id, repository, context).uiState.test {
            awaitItem()
            val unavailable = awaitItem() as DetailUiState.Unavailable
            assertEquals(MessageUnavailableReason.DELETED, unavailable.reason)
        }
        db.close()
    }
}
