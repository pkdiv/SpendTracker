package dev.pkdiv.spendtracker.ui.settings

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesAppVersionFromPackageMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val viewModel = SettingsViewModel(context, SettingsStore(context))

        assertEquals("0.1.0", viewModel.uiState.value.appVersion)
    }
}
