package dev.pkdiv.spendtracker.work

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReportSchedulerTest {

    @Test
    fun `schedules later today when the time has not passed`() {
        val now = LocalDateTime.of(2026, 10, 24, 15, 0)

        assertEquals(
            LocalDateTime.of(2026, 10, 24, 21, 30),
            nextEodRun(now, hour = 21, minute = 30),
        )
    }

    @Test
    fun `schedules tomorrow when the time has already passed`() {
        val now = LocalDateTime.of(2026, 10, 24, 22, 0)

        assertEquals(
            LocalDateTime.of(2026, 10, 25, 21, 30),
            nextEodRun(now, hour = 21, minute = 30),
        )
    }

    @Test
    fun `keeps today when now is exactly the configured time`() {
        val now = LocalDateTime.of(2026, 10, 24, 21, 30)

        assertEquals(
            LocalDateTime.of(2026, 10, 24, 21, 30),
            nextEodRun(now, hour = 21, minute = 30),
        )
    }
}
