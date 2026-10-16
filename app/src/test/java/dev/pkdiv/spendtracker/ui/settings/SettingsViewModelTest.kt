package dev.pkdiv.spendtracker.ui.settings

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.SettingsStore
import dev.pkdiv.spendtracker.testutil.ViewModelTest
import org.junit.Assert.assertEquals
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

        val viewModel = track(SettingsViewModel(context, SettingsStore(context)))

        assertEquals("0.1.0", viewModel.uiState.value.appVersion)
    }
}
