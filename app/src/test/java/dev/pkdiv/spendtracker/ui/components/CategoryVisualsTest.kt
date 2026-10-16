package dev.pkdiv.spendtracker.ui.components

import dev.pkdiv.spendtracker.parsing.TransactionCategory
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CategoryVisualsTest {

    @Test
    fun duplicateRowsAreMuted() {
        assertTrue(isMutedRow(TransactionCategory.FOOD, isDuplicate = true))
    }

    @Test
    fun selfTransferRowsAreMuted() {
        assertTrue(isMutedRow(TransactionCategory.SELF_TRANSFER, isDuplicate = false))
    }

    @Test
    fun regularRowsAreNotMuted() {
        assertFalse(isMutedRow(TransactionCategory.SHOPPING, isDuplicate = false))
    }
}
