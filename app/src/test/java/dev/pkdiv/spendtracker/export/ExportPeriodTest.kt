package dev.pkdiv.spendtracker.export

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExportPeriodTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 18)

    @Test
    fun `this month spans the current calendar month`() {
        val (from, to) = ExportPeriod.THIS_MONTH.bounds(today, zone)!!

        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), from)
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), to)
    }

    @Test
    fun `last month spans the previous calendar month`() {
        val (from, to) = ExportPeriod.LAST_MONTH.bounds(today, zone)!!

        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), from)
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), to)
    }

    @Test
    fun `all time is unbounded`() {
        assertNull(ExportPeriod.ALL_TIME.bounds(today, zone))
    }
}
