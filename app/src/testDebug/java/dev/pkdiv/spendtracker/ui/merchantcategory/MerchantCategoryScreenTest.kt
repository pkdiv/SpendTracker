package dev.pkdiv.spendtracker.ui.merchantcategory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.ui.components.PreviewData
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Editing flows on the merchant -> category editor. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MerchantCategoryScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private data class Save(val original: String?, val merchant: String, val category: TransactionCategory)

    @Test
    fun rendersMappings() {
        compose.setContent {
            SpendTrackerTheme {
                MerchantCategoryContent(
                    state = MerchantCategoryUiState.Content(PreviewData.merchantMappings),
                    onBack = {},
                    onSave = { _, _, _ -> },
                    onDelete = {},
                )
            }
        }

        compose.onNodeWithText("SWIGGY").assertIsDisplayed()
        compose.onNodeWithText("Food").assertIsDisplayed()
    }

    @Test
    fun emptyState_whenNoMappings() {
        compose.setContent {
            SpendTrackerTheme {
                MerchantCategoryContent(
                    state = MerchantCategoryUiState.Content(emptyList()),
                    onBack = {},
                    onSave = { _, _, _ -> },
                    onDelete = {},
                )
            }
        }

        compose.onNodeWithText("No merchant rules yet").assertIsDisplayed()
    }

    @Test
    fun addMapping_recordsNewKey() {
        var saved: Save? = null
        compose.setContent {
            SpendTrackerTheme {
                MerchantCategoryContent(
                    state = MerchantCategoryUiState.Content(emptyList()),
                    onBack = {},
                    onSave = { original, merchant, category -> saved = Save(original, merchant, category) },
                    onDelete = {},
                )
            }
        }

        compose.onNodeWithText("Add mapping", useUnmergedTree = true).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Zomato")
        compose.onNodeWithText("Bills").performClick()
        compose.onNodeWithText("Save").performClick()

        assertEquals(Save(null, "Zomato", TransactionCategory.BILLS), saved)
    }

    @Test
    fun editingMapping_recordsOriginalKey() {
        var saved: Save? = null
        compose.setContent {
            SpendTrackerTheme {
                MerchantCategoryContent(
                    state = MerchantCategoryUiState.Content(PreviewData.merchantMappings),
                    onBack = {},
                    onSave = { original, merchant, category -> saved = Save(original, merchant, category) },
                    onDelete = {},
                )
            }
        }

        compose.onNodeWithText("SWIGGY").performClick()
        compose.onNodeWithText("Health").performClick()
        compose.onNodeWithText("Save").performClick()

        assertEquals(Save("SWIGGY", "SWIGGY", TransactionCategory.HEALTH), saved)
    }

    @Test
    fun deleteMapping_recordsMerchant() {
        var deleted: String? = null
        compose.setContent {
            SpendTrackerTheme {
                MerchantCategoryContent(
                    state = MerchantCategoryUiState.Content(PreviewData.merchantMappings),
                    onBack = {},
                    onSave = { _, _, _ -> },
                    onDelete = { deleted = it },
                )
            }
        }

        compose.onNodeWithContentDescription("Remove SWIGGY").performClick()

        assertEquals("SWIGGY", deleted)
    }
}
