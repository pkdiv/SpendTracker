package dev.pkdiv.spendtracker.ui

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
}
