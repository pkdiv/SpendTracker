package dev.pkdiv.spendtracker.ingestion.sms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The inbox scan resumes above the persisted high-water mark instead of re-reading it. */
class SmsScanWindowTest {

    @Test
    fun `selects only ids above the high-water mark`() {
        val window = SmsScanWindow(afterId = 42)

        assertEquals("_id > ?", window.selection)
        assertEquals(listOf("42"), window.selectionArgs.toList())
        assertEquals("_id ASC", window.sortOrder)
    }

    @Test
    fun `high-water mark only moves forward`() {
        assertEquals(10L, SmsScanWindow.nextHighWaterMark(current = 10, smsId = 7))
        assertEquals(12L, SmsScanWindow.nextHighWaterMark(current = 10, smsId = 12))
    }
}
