package dev.pkdiv.spendtracker.reports

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Finds subscriptions and other recurring spend: the same merchant and amount
 * appearing repeatedly at a regular cadence. This is a passive insight only —
 * it never schedules reminders or nudges the user.
 *
 * A (merchant, amount) pair is considered recurring when it has at least three
 * occurrences whose consecutive gaps all fall in one cadence band (monthly or
 * weekly), and the most recent occurrence is recent enough to be "active".
 */
object RecurringSpendDetector {

    private const val MIN_OCCURRENCES = 3

    private const val MONTHLY_MIN_GAP_DAYS = 25
    private const val MONTHLY_MAX_GAP_DAYS = 35
    private const val WEEKLY_MIN_GAP_DAYS = 5
    private const val WEEKLY_MAX_GAP_DAYS = 9

    fun detect(
        transactions: List<TransactionEntity>,
        now: LocalDate,
        zone: ZoneId,
    ): List<RecurringSpend> {
        val spend = transactions.filter {
            it.direction == TransactionDirection.DEBIT &&
                it.category != TransactionCategory.SELF_TRANSFER &&
                !it.isDuplicate
        }

        return spend
            .groupBy { it.merchant to it.amount }
            .mapNotNull { (key, group) ->
                val (merchant, amount) = key
                val dates = group.map { it.timestamp.atZone(zone).toLocalDate() }.sorted()
                val cadence = cadenceFor(dates) ?: return@mapNotNull null
                if (!isActive(dates.last(), cadence, now)) return@mapNotNull null
                RecurringSpend(
                    cadence = cadence,
                    merchant = merchant,
                    amount = amount,
                    occurrences = dates.size,
                    lastSeen = dates.last(),
                )
            }
            .sortedWith(compareByDescending<RecurringSpend> { it.amount }.thenBy { it.merchant })
    }

    private fun cadenceFor(dates: List<LocalDate>): RecurringCadence? {
        if (dates.size < MIN_OCCURRENCES) return null
        val gaps = dates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        return when {
            gaps.all { it in MONTHLY_MIN_GAP_DAYS..MONTHLY_MAX_GAP_DAYS } -> RecurringCadence.MONTHLY
            gaps.all { it in WEEKLY_MIN_GAP_DAYS..WEEKLY_MAX_GAP_DAYS } -> RecurringCadence.WEEKLY
            else -> null
        }
    }

    private fun isActive(lastSeen: LocalDate, cadence: RecurringCadence, now: LocalDate): Boolean {
        val periodDays = when (cadence) {
            RecurringCadence.MONTHLY -> 30
            RecurringCadence.WEEKLY -> 7
        }
        return ChronoUnit.DAYS.between(lastSeen, now) <= periodDays * 2
    }
}
