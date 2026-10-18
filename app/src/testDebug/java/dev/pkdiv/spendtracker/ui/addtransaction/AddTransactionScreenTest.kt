package dev.pkdiv.spendtracker.ui.addtransaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
import dev.pkdiv.spendtracker.util.AmountInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Entry-form interactions. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AddTransactionScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private data class Entry(
        val amount: String,
        val merchant: String,
        val direction: TransactionDirection,
        val category: TransactionCategory,
    )

    @Test
    fun entryForm_collectsInputAndSaves() {
        var saved: Entry? = null
        compose.setContent {
            var form by remember { mutableStateOf(ManualTransactionForm()) }
            SpendTrackerTheme {
                AddTransactionContent(
                    state = AddTransactionUiState.Editing(form),
                    onAmountChange = { form = form.copy(amount = AmountInput.sanitize(it)) },
                    onDirectionChange = { form = form.copy(direction = it) },
                    onMerchantChange = { form = form.copy(merchant = it) },
                    onCategoryChange = { form = form.copy(category = it) },
                    onAccountChange = { form = form.copy(account = it) },
                    onDateChange = { form = form.copy(date = it) },
                    onSave = {
                        saved = Entry(form.amount, form.merchant, form.direction, form.category)
                    },
                    onBack = {},
                )
            }
        }

        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("450")
        fields[1].performTextInput("Chai stall")
        compose.onNodeWithText("Food").performClick()
        compose.onNodeWithText("Money in").performClick()
        compose.onNodeWithText("Save").performClick()

        assertEquals(
            Entry("450", "Chai stall", TransactionDirection.CREDIT, TransactionCategory.FOOD),
            saved,
        )
    }

    @Test
    fun showsErrors_whenTheStateHasThem() {
        compose.setContent {
            SpendTrackerTheme {
                AddTransactionContent(
                    state = AddTransactionUiState.Editing(
                        form = ManualTransactionForm(),
                        errors = ManualTransactionErrors(amount = true, merchant = true),
                    ),
                    onAmountChange = {},
                    onDirectionChange = {},
                    onMerchantChange = {},
                    onCategoryChange = {},
                    onAccountChange = {},
                    onDateChange = {},
                    onSave = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Enter an amount greater than zero").assertIsDisplayed()
        compose.onNodeWithText("Enter a merchant or payee").assertIsDisplayed()
    }
}
