package dev.pkdiv.spendtracker.ui.history

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HistoryFiltersTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private var counter = 0

    private fun tx(
        merchant: String = "Swiggy",
        amount: String = "100.00",
        direction: TransactionDirection = TransactionDirection.DEBIT,
        category: TransactionCategory = TransactionCategory.FOOD,
        date: LocalDate = LocalDate.of(2026, 10, 15),
        isDuplicate: Boolean = false,
    ) = TransactionEntity(
        amount = BigDecimal(amount),
        direction = direction,
        merchant = merchant,
        account = null,
        category = category,
        timestamp = date.atStartOfDay(zone).toInstant(),
        rawMessageRef = "ref:${counter++}",
        isDuplicate = isDuplicate,
    )

    @Test
    fun `no criteria returns every transaction`() {
        val rows = listOf(tx(), tx(merchant = "Uber"))

        assertEquals(rows, filterTransactions(rows, HistoryFilters(), zone))
    }

    @Test
    fun `filters by direction`() {
        val rows = listOf(
            tx(direction = TransactionDirection.DEBIT),
            tx(direction = TransactionDirection.CREDIT),
        )

        val creditOnly = filterTransactions(rows, HistoryFilters(direction = TransactionDirection.CREDIT), zone)

        assertEquals(listOf(rows[1]), creditOnly)
    }

    @Test
    fun `date range is inclusive on both ends`() {
        val rows = listOf(
            tx(date = LocalDate.of(2026, 10, 10)),
            tx(date = LocalDate.of(2026, 10, 15)),
            tx(date = LocalDate.of(2026, 10, 20)),
        )

        val filtered = filterTransactions(
            rows,
            HistoryFilters(fromDate = LocalDate.of(2026, 10, 15), toDate = LocalDate.of(2026, 10, 20)),
            zone,
        )

        assertEquals(listOf(rows[1], rows[2]), filtered)
    }

    @Test
    fun `filters by amount range`() {
        val rows = listOf(tx(amount = "50"), tx(amount = "500"), tx(amount = "5000"))

        val filtered = filterTransactions(
            rows,
            HistoryFilters(minAmountInput = "100", maxAmountInput = "1000"),
            zone,
        )

        assertEquals(listOf(rows[1]), filtered)
    }

    @Test
    fun `excludeDuplicates hides flagged rows`() {
        val rows = listOf(tx(), tx(merchant = "Uber", isDuplicate = true))

        val filtered = filterTransactions(rows, HistoryFilters(excludeDuplicates = true), zone)

        assertEquals(listOf(rows[0]), filtered)
    }

    @Test
    fun `combines query category and amount`() {
        val rows = listOf(
            tx(merchant = "Swiggy", amount = "400"),
            tx(merchant = "Swiggy", amount = "4000"),
            tx(merchant = "Zomato", amount = "400"),
        )

        val filtered = filterTransactions(
            rows,
            HistoryFilters(query = "swig", category = TransactionCategory.FOOD, maxAmountInput = "1000"),
            zone,
        )

        assertEquals(listOf(rows[0]), filtered)
    }

    @Test
    fun `unparseable amount input is ignored`() {
        val rows = listOf(tx(amount = "50"), tx(amount = "5000"))

        val filtered = filterTransactions(rows, HistoryFilters(minAmountInput = ".."), zone)

        assertEquals(rows, filtered)
    }

    @Test
    fun `activeFilterCount counts each filter group once`() {
        val filters = HistoryFilters(
            query = "swiggy",
            category = TransactionCategory.FOOD,
            direction = TransactionDirection.DEBIT,
            fromDate = LocalDate.of(2026, 10, 1),
            toDate = LocalDate.of(2026, 10, 31),
            minAmountInput = "100",
            maxAmountInput = "500",
            excludeDuplicates = true,
        )

        assertEquals(5, filters.activeFilterCount)
        assertEquals(0, HistoryFilters().activeFilterCount)
        assertEquals(0, HistoryFilters(query = "x").activeFilterCount)
        assertEquals(true, HistoryFilters(query = "x").hasAnyCriteria)
    }
}
