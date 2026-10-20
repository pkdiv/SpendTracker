package dev.pkdiv.spendtracker.ui.export

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pkdiv.spendtracker.export.ExportPeriod
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Export screen interactions. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ExportContentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun selectingPeriod_reportsIt() {
        var selected: ExportPeriod? = null
        compose.setContent {
            SpendTrackerTheme {
                ExportContent(
                    state = ExportUiState.Content(ExportPeriod.THIS_MONTH, transactionCount = 12),
                    onPeriodChange = { selected = it },
                    onExport = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Last month").performClick()

        assertEquals(ExportPeriod.LAST_MONTH, selected)
    }

    @Test
    fun emptyPeriod_disablesExport() {
        compose.setContent {
            SpendTrackerTheme {
                ExportContent(
                    state = ExportUiState.Content(ExportPeriod.LAST_MONTH, transactionCount = 0),
                    onPeriodChange = {},
                    onExport = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("No transactions in this period").assertIsDisplayed()
        compose.onNodeWithText("Export CSV").assertIsNotEnabled()
    }

    @Test
    fun populatedPeriod_reportsCount_andFiresExport() {
        var exports = 0
        compose.setContent {
            SpendTrackerTheme {
                ExportContent(
                    state = ExportUiState.Content(ExportPeriod.THIS_MONTH, transactionCount = 12),
                    onPeriodChange = {},
                    onExport = { exports++ },
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("12 transactions").assertIsDisplayed()
        compose.onNodeWithText("Export CSV").assertIsEnabled().performClick()

        assertEquals(1, exports)
    }
}
