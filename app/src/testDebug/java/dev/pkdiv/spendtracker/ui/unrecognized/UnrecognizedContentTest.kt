package dev.pkdiv.spendtracker.ui.unrecognized

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageEntity
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Alerts header's clear-all action and its confirmation dialog. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class UnrecognizedContentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun alert(id: Long, sender: String) = UnrecognizedMessageEntity(
        id = id,
        sender = sender,
        body = "Rs.100 debited",
        rawMessageRef = "ref:$id",
        receivedAt = Instant.ofEpochMilli(1700000000000),
    )

    private fun setContent(
        state: UnrecognizedUiState,
        onDismiss: (Long) -> Unit = {},
        onClearAll: () -> Unit = {},
    ) {
        compose.setContent {
            SpendTrackerTheme {
                UnrecognizedContent(state = state, onDismiss = onDismiss, onClearAll = onClearAll)
            }
        }
    }

    @Test
    fun clearAll_asksForConfirmation_thenInvokesCallback() {
        var cleared = false
        setContent(UnrecognizedUiState(listOf(alert(1, "HDFCBK"))), onClearAll = { cleared = true })

        compose.onNodeWithText("Clear all").performClick()
        compose.onNodeWithText("Clear all alerts?").assertIsDisplayed()
        compose.onNodeWithText("Clear").performClick()

        assertEquals(true, cleared)
    }

    @Test
    fun dismissingTheDialog_doesNotClear() {
        var cleared = false
        setContent(UnrecognizedUiState(listOf(alert(1, "HDFCBK"))), onClearAll = { cleared = true })

        compose.onNodeWithText("Clear all").performClick()
        compose.onNodeWithText("Cancel").performClick()

        assertEquals(false, cleared)
        compose.onNodeWithText("Clear all alerts?").assertDoesNotExist()
    }

    @Test
    fun dismiss_reportsTheAlertId() {
        var dismissed: Long? = null
        setContent(UnrecognizedUiState(listOf(alert(7, "HDFCBK"))), onDismiss = { dismissed = it })

        compose.onNodeWithText("Dismiss").performClick()

        assertEquals(7L, dismissed)
    }

    @Test
    fun longMessage_collapsesThenExpands() {
        val longBody = "Rs.100 debited from your account. " +
            "Additional transaction context that wraps across several lines. ".repeat(20)
        setContent(UnrecognizedUiState(listOf(alert(1, "HDFCBK").copy(body = longBody))))

        compose.onNodeWithText("Show full message").assertIsDisplayed()
        compose.onNodeWithText("Show full message").performClick()
        compose.onNodeWithText("Show less").assertExists()
    }

    @Test
    fun shortMessage_hidesTheExpandToggle() {
        setContent(UnrecognizedUiState(listOf(alert(1, "HDFCBK"))))

        compose.onNodeWithText("Show full message").assertDoesNotExist()
    }

    @Test
    fun emptyState_hidesClearAll() {
        setContent(UnrecognizedUiState(emptyList()))

        compose.onNodeWithText("Clear all").assertDoesNotExist()
        compose.onNodeWithText("You're all caught up").assertIsDisplayed()
    }
}
