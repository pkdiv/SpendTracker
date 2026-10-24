package dev.pkdiv.spendtracker.ui.home

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.SettingsStore
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.ingestion.sms.SmsProcessor
import dev.pkdiv.spendtracker.ingestion.sms.SmsReader
import dev.pkdiv.spendtracker.parsing.ParserEngine
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionCategorizer
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.testutil.ViewModelTest
import dev.pkdiv.spendtracker.widget.WidgetRefresher
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HomeViewModelTest : ViewModelTest() {

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

    private val widgetRefresher = object : WidgetRefresher {
        override fun refresh() = Unit
    }

    private fun viewModel(context: Context, repository: TransactionRepository): HomeViewModel =
        track(
            HomeViewModel(
                context,
                repository,
                SmsReader(
                    context,
                    SmsProcessor(ParserEngine(), repository, widgetRefresher),
                    SettingsStore(context),
                ),
            ),
        )

    private var counter = 0

    private fun rowAt(amount: String, date: LocalDate, merchant: String = "Swiggy") = TransactionEntity(
        amount = BigDecimal(amount),
        direction = TransactionDirection.DEBIT,
        merchant = merchant,
        account = null,
        category = TransactionCategory.FOOD,
        timestamp = date.atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant(),
        rawMessageRef = "ref:${counter++}",
    )

    private fun row(amount: String, daysAgo: Long) = rowAt(amount, LocalDate.now().minusDays(daysAgo))

    @Test
    fun weekPagerCoversTheCurrentWeekAndThePreviousThree() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (db, repository) = openRepository()
        db.transactionDao().insertAll(
            listOf(
                row("100.00", 0),
                row("200.00", 7),
                row("300.00", 14),
                row("400.00", 21),
                row("500.00", 28),
            ),
        )
        val viewModel = viewModel(context, repository)

        val state = viewModel.uiState.first { it.weeks.size == 4 && it.weeks[0].totalSpend.signum() > 0 }

        assertEquals(listOf(0, 1, 2, 3), state.weeks.map { it.weeksAgo })
        assertEquals(BigDecimal("100.00"), state.weeks[0].totalSpend)
        assertEquals(BigDecimal("200.00"), state.weeks[1].totalSpend)
        assertEquals(BigDecimal("300.00"), state.weeks[2].totalSpend)
        assertEquals(BigDecimal("400.00"), state.weeks[3].totalSpend)
        assertEquals(BigDecimal("1000.00"), state.weeks.fold(BigDecimal.ZERO) { acc, week -> acc + week.totalSpend })
        assertEquals(BigDecimal("100.00"), state.weekSpend)
        assertEquals(7, state.weeks[0].dailySpend.size)
        assertEquals(BigDecimal("100.00"), state.weeks[0].dailySpend[LocalDate.now().dayOfWeek.value - 1])
        db.close()
    }

    @Test
    fun weekSpendStaysOnTheCurrentWeekWhenAPastMonthIsSelected() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (db, repository) = openRepository()
        db.transactionDao().insertAll(
            listOf(
                row("100.00", 0),
                rowAt("500.00", YearMonth.now().minusMonths(1).atDay(15)),
            ),
        )
        val viewModel = viewModel(context, repository)

        val current = viewModel.uiState.first { it.weeks.size == 4 && it.totalSpend.signum() > 0 }
        assertEquals(BigDecimal("100.00"), current.totalSpend)
        assertEquals(BigDecimal("100.00"), current.weekSpend)

        viewModel.selectMonth(YearMonth.now().minusMonths(1))

        val past = viewModel.uiState.first { it.totalSpend == BigDecimal("500.00") }
        assertEquals(BigDecimal("500.00"), past.totalSpend)
        assertEquals(BigDecimal("100.00"), past.weekSpend)
        assertEquals(BigDecimal("100.00"), past.weeks[0].totalSpend)
        db.close()
    }
}
