package dev.pkdiv.spendtracker.ui.history

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Activity filter controls report hoisted callbacks. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HistoryFilterContentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        filters: HistoryFilters = HistoryFilters(),
        onDirectionChange: (TransactionDirection?) -> Unit = {},
        onMinAmountChange: (String) -> Unit = {},
        onExcludeDuplicatesChange: (Boolean) -> Unit = {},
        onClearFilters: () -> Unit = {},
    ) {
        compose.setContent {
            SpendTrackerTheme {
                HistoryFilterContent(
                    filters = filters,
                    onDirectionChange = onDirectionChange,
                    onDateRangeChange = { _, _ -> },
                    onMinAmountChange = onMinAmountChange,
                    onMaxAmountChange = {},
                    onExcludeDuplicatesChange = onExcludeDuplicatesChange,
                    onClearFilters = onClearFilters,
                )
            }
        }
    }

    @Test
    fun directionChip_reportsSelection() {
        var selected: TransactionDirection? = null
        setContent(onDirectionChange = { selected = it })

        compose.onNodeWithText("Money out").performClick()

        assertEquals(TransactionDirection.DEBIT, selected)
    }

    @Test
    fun duplicateSwitch_reportsToggle() {
        var toggled: Boolean? = null
        setContent(onExcludeDuplicatesChange = { toggled = it })

        compose.onNode(isToggleable()).performClick()

        assertEquals(true, toggled)
    }

    @Test
    fun amountField_reportsInput() {
        var min: String? = null
        setContent(onMinAmountChange = { min = it })

        compose.onAllNodes(hasSetTextAction())[0].performTextInput("250")

        assertEquals("250", min)
    }

    @Test
    fun clearButton_reportsClear() {
        var cleared = false
        setContent(onClearFilters = { cleared = true })

        compose.onNodeWithText("Clear filters").performClick()

        assertEquals(true, cleared)
    }
}
