package dev.pkdiv.spendtracker.ui.detail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pkdiv.spendtracker.ui.components.OriginalMessageCard
import dev.pkdiv.spendtracker.ui.components.PreviewData
import dev.pkdiv.spendtracker.ui.components.TransactionRow
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Row click routes to the detail view, which renders the original message body. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TransactionDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rowClick_opensDetail_andRendersRawBody() {
        val selectedId = mutableStateOf<Long?>(null)

        compose.setContent {
            SpendTrackerTheme {
                val id = selectedId.value
                if (id == null) {
                    TransactionRow(
                        transaction = PreviewData.transactions.first(),
                        onTransactionClick = { selectedId.value = it },
                        onCategoryChange = {},
                        onToggleDuplicate = { _, _ -> },
                    )
                } else {
                    OriginalMessageCard(detail = PreviewData.smsDetail)
                }
            }
        }

        compose.onNodeWithText("Swiggy").performClick()
        compose.onNodeWithText("Rs.450 debited", substring = true).assertIsDisplayed()
    }

    @Test
    fun duplicateRow_showsDuplicateBadge() {
        compose.setContent {
            SpendTrackerTheme {
                TransactionRow(
                    transaction = PreviewData.transactions.first { it.isDuplicate },
                    onTransactionClick = {},
                    onCategoryChange = {},
                    onToggleDuplicate = { _, _ -> },
                )
            }
        }

        compose.onNodeWithText("Duplicate").assertIsDisplayed()
    }

    @Test
    fun detailContent_showsNoteSection() {
        compose.setContent {
            SpendTrackerTheme {
                DetailContent(detail = PreviewData.notedDetail)
            }
        }

        compose.onNodeWithText("Split with Ravi, settled later").assertIsDisplayed()
    }

    @Test
    fun renameDialog_disablesSaveUntilChanged() {
        var saved: String? = null
        compose.setContent {
            SpendTrackerTheme {
                RenameMerchantDialog(initial = "Swiggy", onDismiss = {}, onSave = { saved = it })
            }
        }

        compose.onNodeWithText("Save").assertIsNotEnabled()

        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithText("Save").assertIsNotEnabled()

        compose.onNode(hasSetTextAction()).performTextInput("Zomato")
        compose.onNodeWithText("Save").assertIsEnabled()
        compose.onNodeWithText("Save").performClick()

        assertEquals("Zomato", saved)
    }

    @Test
    fun noteDialog_returnsEnteredText() {
        var saved: String? = null
        compose.setContent {
            SpendTrackerTheme {
                TransactionNoteDialog(initial = null, onDismiss = {}, onSave = { saved = it })
            }
        }

        compose.onNode(hasSetTextAction()).performTextInput("Split with Ravi")
        compose.onNodeWithText("Save").performClick()

        assertEquals("Split with Ravi", saved)
    }

    @Test
    fun noteDialog_clearsOnBlank() {
        var saved: String? = "unchanged"
        compose.setContent {
            SpendTrackerTheme {
                TransactionNoteDialog(initial = "Existing note", onDismiss = {}, onSave = { saved = it })
            }
        }

        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithText("Save").performClick()

        assertNull(saved)
    }
}
