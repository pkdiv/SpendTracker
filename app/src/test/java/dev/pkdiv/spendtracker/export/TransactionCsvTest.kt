package dev.pkdiv.spendtracker.export

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TransactionCsvTest {

    private val zone = ZoneId.of("UTC")

    private fun txn(
        amount: String = "450.00",
        merchant: String = "Swiggy",
        direction: TransactionDirection = TransactionDirection.DEBIT,
        category: TransactionCategory = TransactionCategory.FOOD,
        account: String? = null,
        source: TransactionSource = TransactionSource.SMS,
        at: Instant = Instant.parse("2026-10-14T10:30:00Z"),
    ) = TransactionEntity(
        amount = BigDecimal(amount),
        direction = direction,
        merchant = merchant,
        account = account,
        category = category,
        timestamp = at,
        rawMessageRef = "ref",
        rawSender = "HDFCBK",
        rawBody = "Rs.450 debited from A/c 1234. OTP is 123456.",
        source = source,
    )

    @Test
    fun `writes a header and a row per transaction`() {
        val lines = TransactionCsv.format(listOf(txn(account = "1234")), zone).trim().lines()

        assertEquals("date,direction,amount,merchant,category,account,source", lines[0])
        assertEquals("2026-10-14 10:30,DEBIT,450.00,Swiggy,FOOD,1234,SMS", lines[1])
    }

    @Test
    fun `never includes raw message content`() {
        val csv = TransactionCsv.format(listOf(txn()), zone)

        assertFalse(csv.contains("HDFCBK"))
        assertFalse(csv.contains("OTP"))
        assertFalse(csv.contains("debited"))
    }

    @Test
    fun `escapes commas quotes and newlines`() {
        val csv = TransactionCsv.format(listOf(txn(merchant = "Coffee, \"Bean\"\nLtd")), zone)

        assertTrue(csv.contains("\"Coffee, \"\"Bean\"\"\nLtd\""))
    }

    @Test
    fun `orders rows by timestamp`() {
        val early = txn(merchant = "Early", at = Instant.parse("2026-10-01T09:00:00Z"))
        val late = txn(merchant = "Late", at = Instant.parse("2026-10-20T09:00:00Z"))

        val lines = TransactionCsv.format(listOf(late, early), zone).trim().lines()

        assertTrue(lines[1].contains("Early"))
        assertTrue(lines[2].contains("Late"))
    }

    @Test
    fun `empty list is just the header`() {
        assertEquals(
            "date,direction,amount,merchant,category,account,source",
            TransactionCsv.format(emptyList(), zone).trim(),
        )
    }
}
