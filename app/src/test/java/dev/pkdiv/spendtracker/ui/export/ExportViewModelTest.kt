package dev.pkdiv.spendtracker.ui.export

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.export.ExportFileWriter
import dev.pkdiv.spendtracker.export.ExportPeriod
import dev.pkdiv.spendtracker.export.ExportedFile
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionCategorizer
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.testutil.ViewModelTest
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ExportViewModelTest : ViewModelTest() {

    private lateinit var db: SpendTrackerDatabase
    private lateinit var repository: TransactionRepository
    private val writer = RecordingWriter()

    private fun openRepository() {
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
    }

    private fun viewModel() = track(ExportViewModel(repository, writer))

    private var counter = 0

    private fun row(amount: String, at: Instant) = TransactionEntity(
        amount = BigDecimal(amount),
        direction = TransactionDirection.DEBIT,
        merchant = "Merchant",
        account = null,
        category = TransactionCategory.FOOD,
        timestamp = at,
        rawMessageRef = "ref:${counter++}",
        source = TransactionSource.SMS,
        receivedAt = at.toEpochMilli(),
    )

    @Test
    fun countsOnlyTransactionsInTheSelectedPeriod() = runBlocking {
        openRepository()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val thisMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().plusSeconds(3600)
        val lastMonth = today.minusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant()
            .plusSeconds(3600)
        db.transactionDao().insert(row("10.00", thisMonth))
        db.transactionDao().insert(row("20.00", thisMonth))
        db.transactionDao().insert(row("30.00", lastMonth))

        val viewModel = viewModel()

        val thisMonthState = viewModel.uiState.first {
            it is ExportUiState.Content && it.period == ExportPeriod.THIS_MONTH && it.transactionCount > 0
        } as ExportUiState.Content
        assertEquals(2, thisMonthState.transactionCount)

        viewModel.selectPeriod(ExportPeriod.LAST_MONTH)
        val lastMonthState = viewModel.uiState.first {
            it is ExportUiState.Content && it.period == ExportPeriod.LAST_MONTH && it.transactionCount > 0
        } as ExportUiState.Content
        assertEquals(1, lastMonthState.transactionCount)
        db.close()
    }

    @Test
    fun exportWritesCsvAndSignalsShareable() = runBlocking {
        openRepository()
        val zone = ZoneId.systemDefault()
        val at = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant().plusSeconds(3600)
        db.transactionDao().insert(row("450.00", at))

        val viewModel = viewModel()
        viewModel.uiState.first { it is ExportUiState.Content && it.transactionCount > 0 }

        viewModel.export()
        val shareable = viewModel.uiState.first { it is ExportUiState.Shareable } as ExportUiState.Shareable

        assertEquals("spendtracker-this-month.csv", shareable.fileName)
        assertTrue(shareable.uri.toString().startsWith("content://"))
        assertEquals("spendtracker-this-month.csv", writer.fileName)
        assertTrue(writer.contents.orEmpty().contains("450.00"))
        db.close()
    }

    @Test
    fun onSharedReturnsToTheForm() = runBlocking {
        openRepository()
        val zone = ZoneId.systemDefault()
        val at = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant().plusSeconds(3600)
        db.transactionDao().insert(row("450.00", at))

        val viewModel = viewModel()
        viewModel.uiState.first { it is ExportUiState.Content && it.transactionCount > 0 }
        viewModel.export()
        viewModel.uiState.first { it is ExportUiState.Shareable }

        viewModel.onShared()

        val state = viewModel.uiState.first { it is ExportUiState.Content } as ExportUiState.Content
        assertEquals(ExportPeriod.THIS_MONTH, state.period)
        db.close()
    }

    /** Records what the ViewModel asked to write and returns a shareable content uri. */
    private class RecordingWriter : ExportFileWriter {
        var fileName: String? = null
        var contents: String? = null

        override fun write(fileName: String, contents: String): ExportedFile {
            this.fileName = fileName
            this.contents = contents
            return ExportedFile(Uri.parse("content://test/$fileName"), fileName)
        }
    }
}
