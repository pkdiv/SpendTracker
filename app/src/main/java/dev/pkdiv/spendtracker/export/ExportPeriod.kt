package dev.pkdiv.spendtracker.export

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** A span of transactions the user can export. */
enum class ExportPeriod {
    THIS_MONTH,
    LAST_MONTH,
    ALL_TIME,
}

/**
 * Inclusive start / exclusive end of the month containing [today] (or the previous
 * month), or null for [ExportPeriod.ALL_TIME], which is unbounded.
 */
fun ExportPeriod.bounds(today: LocalDate, zone: ZoneId): Pair<Instant, Instant>? = when (this) {
    ExportPeriod.THIS_MONTH -> monthBounds(YearMonth.from(today), zone)
    ExportPeriod.LAST_MONTH -> monthBounds(YearMonth.from(today).minusMonths(1), zone)
    ExportPeriod.ALL_TIME -> null
}

private fun monthBounds(month: YearMonth, zone: ZoneId): Pair<Instant, Instant> =
    month.atDay(1).atStartOfDay(zone).toInstant() to
        month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
