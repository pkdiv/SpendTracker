package dev.pkdiv.spendtracker.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormattersTest {

    @Test
    fun merchantLabelReturnsParsedMerchantUnchanged() {
        assertEquals("Swiggy", Formatters.merchantLabel("Swiggy", "SWIGGY"))
    }

    @Test
    fun merchantLabelLeadsWithSenderWhenMerchantIsUnknown() {
        assertEquals("HDFC-BANK (Unknown)", Formatters.merchantLabel("Unknown", "HDFC-BANK"))
    }

    @Test
    fun merchantLabelKeepsUnknownWhenSenderIsMissing() {
        assertEquals("Unknown", Formatters.merchantLabel("Unknown", null))
    }

    @Test
    fun dateRangeLabelCollapsesToDayNumbersWithinAMonth() {
        assertEquals(
            "14–${Formatters.dateLabel(LocalDate.of(2026, 10, 20))}",
            Formatters.dateRangeLabel(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 20)),
        )
    }

    @Test
    fun dateRangeLabelShowsBothDatesWhenTheRangeCrossesAMonth() {
        assertEquals(
            "${Formatters.dateLabel(LocalDate.of(2026, 9, 28))}–${Formatters.dateLabel(LocalDate.of(2026, 10, 4))}",
            Formatters.dateRangeLabel(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4)),
        )
    }

    @Test
    fun weekdayShortLabelsAreMondayFirstAndSevenLong() {
        val labels = Formatters.weekdayShortLabels()

        assertEquals(7, labels.size)
        assertEquals(DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT, Locale.getDefault()), labels.first())
        assertEquals(DayOfWeek.SUNDAY.getDisplayName(TextStyle.SHORT, Locale.getDefault()), labels.last())
    }
}
