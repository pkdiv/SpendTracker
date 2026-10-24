package dev.pkdiv.spendtracker.ui.settings

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.SettingsStore
import dev.pkdiv.spendtracker.testutil.ViewModelTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsViewModelTest : ViewModelTest() {

    @Test
    fun exposesAppVersionFromPackageMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val versionName = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName

        val viewModel = track(SettingsViewModel(context, SettingsStore(context)))

        assertNotNull(versionName)
        assertTrue(versionName!!.matches(Regex("""\d+\.\d+\.\d+""")))
        assertEquals(versionName, viewModel.uiState.value.appVersion)
    }
}
