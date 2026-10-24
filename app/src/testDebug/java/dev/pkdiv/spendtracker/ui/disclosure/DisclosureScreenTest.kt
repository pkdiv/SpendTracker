package dev.pkdiv.spendtracker.ui.disclosure

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The prominent SMS disclosure rendered before any runtime permission prompt. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DisclosureScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        onAllowSms: () -> Unit = {},
        onContinueWithoutSms: () -> Unit = {},
    ) {
        compose.setContent {
            SpendTrackerTheme {
                DisclosureScreen(
                    onAllowSms = onAllowSms,
                    onContinueWithoutSms = onContinueWithoutSms,
                )
            }
        }
    }

    @Test
    fun disclosesWhatIsAccessedAndThatItStaysOnDevice() {
        setContent()

        compose.onNodeWithText("Before you start").assertIsDisplayed()
        compose.onNodeWithText("Read your SMS messages").assertIsDisplayed()
        compose.onNodeWithText("Send notifications").assertIsDisplayed()
        compose.onNodeWithText("never leave your phone", substring = true).assertExists()
    }

    @Test
    fun allowSms_isAffirmativeAction() {
        var allowed = false
        setContent(onAllowSms = { allowed = true })

        compose.onNodeWithText("Allow SMS access").performScrollTo().performClick()

        assertTrue(allowed)
    }

    @Test
    fun continueWithoutSms_keepsTheAppUsable() {
        var continued = false
        setContent(onContinueWithoutSms = { continued = true })

        compose.onNodeWithText("Continue without SMS").performScrollTo().performClick()

        assertTrue(continued)
    }

    @Test
    fun notificationRationale_explainsThenRequests() {
        var allowed = false
        var dismissed = false
        compose.setContent {
            SpendTrackerTheme {
                NotificationRationaleDialog(
                    onAllow = { allowed = true },
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText("Turn on notifications?").assertIsDisplayed()
        compose.onNodeWithText("Allow notifications").performClick()

        assertTrue(allowed)
        assertFalse(dismissed)
    }
}
