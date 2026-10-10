package dev.pkdiv.spendtracker.ui.detail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pkdiv.spendtracker.ui.components.OriginalMessageCard
import dev.pkdiv.spendtracker.ui.components.PreviewData
import dev.pkdiv.spendtracker.ui.components.TransactionRow
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
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
                    )
                } else {
                    OriginalMessageCard(detail = PreviewData.smsDetail)
                }
            }
        }

        compose.onNodeWithText("Swiggy").performClick()
        compose.onNodeWithText("Rs.450 debited", substring = true).assertIsDisplayed()
    }
}
