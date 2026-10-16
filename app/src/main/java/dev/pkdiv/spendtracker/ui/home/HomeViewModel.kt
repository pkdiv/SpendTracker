package dev.pkdiv.spendtracker.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.ingestion.sms.SmsReader
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.reports.CategorySpend
import dev.pkdiv.spendtracker.reports.ReportAggregator
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.components.WeekSpend
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RescanState {
    data object Idle : RescanState
    data object Running : RescanState
    data class Done(val processed: Int) : RescanState
    data object NoPermission : RescanState
    data class Error(val message: String) : RescanState
}

data class TransactionSection(
    val date: LocalDate,
    val transactions: List<TransactionEntity>,
)

data class HomeUiState(
    val monthLabel: String = "",
    val selectedMonth: YearMonth = YearMonth.now(),
    val totalSpend: BigDecimal = BigDecimal.ZERO,
    val previousMonthTotal: BigDecimal = BigDecimal.ZERO,
    val deltaPercent: BigDecimal? = null,
    val transactionCount: Int = 0,
    val dailySpend: List<BigDecimal> = emptyList(),
    val todaySpend: BigDecimal = BigDecimal.ZERO,
    val weekSpend: BigDecimal = BigDecimal.ZERO,
    val weeks: List<WeekSpend> = emptyList(),
    val weekDayLabels: List<String> = emptyList(),
    val dailyAverage: BigDecimal = BigDecimal.ZERO,
    val topCategories: List<CategorySpend> = emptyList(),
    val sections: List<TransactionSection> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val smsReader: SmsReader,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<HomeUiState> = selectedMonth
        .flatMapLatest { month -> observeData(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun observeData(month: YearMonth): kotlinx.coroutines.flow.Flow<HomeUiState> {
        val zone = ZoneOffset.systemDefault()
        val currentStart = month.atDay(1).atStartOfDay(zone).toInstant()
        val currentEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        val previousStart = month.minusMonths(1).atDay(1).atStartOfDay(zone).toInstant()

        // The week card pages through the last four weeks, independent of the month selector.
        val weekStart = weekStartOf(today)
        val weeksWindowStart = weekStart.minusWeeks((WEEKS_SHOWN - 1).toLong()).atStartOfDay(zone).toInstant()
        val weekEndInstant = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant()

        return combine(
            transactionRepository.observeBetween(currentStart, currentEnd),
            transactionRepository.observeBetween(previousStart, currentStart),
            transactionRepository.observeBetween(weeksWindowStart, weekEndInstant),
        ) { current, previous, weeksTxns -> buildState(month, weekStart, current, previous, weeksTxns) }
    }

    private fun buildState(
        month: YearMonth,
        weekStart: LocalDate,
        current: List<TransactionEntity>,
        previous: List<TransactionEntity>,
        weeksTxns: List<TransactionEntity>,
    ): HomeUiState {
        val report = ReportAggregator.monthly(current, previous)
        val zone = ZoneId.systemDefault()

        val debitsByDay = ReportAggregator.spendByDay(current, zone)
        val weeksByDay = ReportAggregator.spendByDay(weeksTxns, zone)

        val isCurrentMonth = month == YearMonth.from(today)
        val daysElapsed = if (isCurrentMonth) today.dayOfMonth else month.lengthOfMonth()
        val dailySpend = (1..daysElapsed).map { day -> debitsByDay[month.atDay(day)] ?: BigDecimal.ZERO }

        val todaySpend = if (isCurrentMonth) debitsByDay[today] ?: BigDecimal.ZERO else BigDecimal.ZERO

        val weeks = (0 until WEEKS_SHOWN).map { offset ->
            val start = weekStart.minusWeeks(offset.toLong())
            val days = (0..6).map { day -> weeksByDay[start.plusDays(day.toLong())] ?: BigDecimal.ZERO }
            WeekSpend(
                weeksAgo = offset,
                rangeLabel = Formatters.dateRangeLabel(start, start.plusDays(6)),
                totalSpend = days.fold(BigDecimal.ZERO) { acc, value -> acc + value },
                transactionCount = weeksTxns.count { txn ->
                    !txn.isDuplicate && weekStartOf(txn.timestamp.atZone(zone).toLocalDate()) == start
                },
                dailySpend = days,
            )
        }

        val dailyAverage = if (daysElapsed > 0) {
            report.totalSpend.divide(BigDecimal(daysElapsed), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val deltaPercent = if (report.previousMonthTotal.signum() == 0) {
            null
        } else {
            report.totalSpend
                .subtract(report.previousMonthTotal)
                .divide(report.previousMonthTotal, 4, RoundingMode.HALF_UP)
                .movePointRight(2)
        }

        return HomeUiState(
            monthLabel = Formatters.monthLabel(month.atDay(1)),
            selectedMonth = month,
            totalSpend = report.totalSpend,
            previousMonthTotal = report.previousMonthTotal,
            deltaPercent = deltaPercent,
            transactionCount = report.transactionCount,
            dailySpend = dailySpend,
            todaySpend = todaySpend,
            weekSpend = weeks.first().totalSpend,
            weeks = weeks,
            weekDayLabels = Formatters.weekdayShortLabels(),
            dailyAverage = dailyAverage,
            topCategories = report.categories.take(4),
            sections = current
                .filter { !it.isDuplicate }
                .groupBy { it.timestamp.atZone(zone).toLocalDate() }
                .toList()
                .sortedByDescending { (date, _) -> date }
                .map { (date, list) -> TransactionSection(date, list) },
        )
    }

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        if (selectedMonth.value < YearMonth.now()) {
            selectedMonth.value = selectedMonth.value.plusMonths(1)
        }
    }

    fun selectMonth(month: YearMonth) {
        if (month <= YearMonth.now()) {
            selectedMonth.value = month
        }
    }

    fun updateCategory(id: Long, category: TransactionCategory) {
        viewModelScope.launch { transactionRepository.updateCategory(id, category) }
    }

    fun setDuplicate(id: Long, isDuplicate: Boolean) {
        viewModelScope.launch { transactionRepository.setDuplicate(id, isDuplicate) }
    }

    private val _rescanState = MutableStateFlow<RescanState>(RescanState.Idle)
    val rescanState: StateFlow<RescanState> = _rescanState.asStateFlow()

    fun consumeRescan() {
        _rescanState.value = RescanState.Idle
    }

    fun rescan() {
        if (_rescanState.value is RescanState.Running) return

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            _rescanState.value = RescanState.NoPermission
            return
        }

        viewModelScope.launch {
            _rescanState.value = RescanState.Running
            runCatching { smsReader.backfill() }
                .onSuccess { count ->
                    _rescanState.value = RescanState.Done(count)
                }
                .onFailure { e ->
                    _rescanState.value = RescanState.Error(e.message ?: "Unknown error")
                }
        }
    }
}

/** How many weeks the Home week card pages through, current week first. */
private const val WEEKS_SHOWN = 4

/** Monday of [date]'s week. */
private fun weekStartOf(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
