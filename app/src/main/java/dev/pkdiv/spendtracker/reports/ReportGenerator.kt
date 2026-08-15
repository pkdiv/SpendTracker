package dev.pkdiv.spendtracker.reports

import dev.pkdiv.spendtracker.data.db.TransactionDao
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportGenerator @Inject constructor(
    private val transactionDao: TransactionDao,
) {
    suspend fun dailyReport(date: LocalDate): DailyReport {
        val from = date.atStartOfDay(ZoneOffset.systemDefault()).toInstant()
        val to = date.plusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant()
        val transactions = transactionDao.between(from, to)
        return ReportAggregator.daily(transactions)
    }

    suspend fun monthlyReport(month: LocalDate): MonthlyReport {
        val start = month.withDayOfMonth(1)
        val from = start.atStartOfDay(ZoneOffset.systemDefault()).toInstant()
        val to = start.plusMonths(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant()
        val prevFrom = start.minusMonths(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant()

        val transactions = transactionDao.between(from, to)
        val previous = transactionDao.between(prevFrom, from)
        return ReportAggregator.monthly(transactions, previous)
    }
}
