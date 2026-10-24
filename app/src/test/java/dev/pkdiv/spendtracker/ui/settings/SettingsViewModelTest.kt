package dev.pkdiv.spendtracker.ui.settings

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.SettingsStore
import dev.pkdiv.spendtracker.testutil.ViewModelTest
import dev.pkdiv.spendtracker.work.ReportScheduling
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

    private class RecordingScheduling : ReportScheduling {
        val eodScheduled = CompletableDeferred<Unit>()
        override fun scheduleEod() {
            eodScheduled.complete(Unit)
        }

        override fun scheduleEom() = Unit

        override fun catchUpMissedReports() = Unit
    }

    private fun viewModel(context: Context, scheduling: ReportScheduling) =
        track(SettingsViewModel(context, SettingsStore(context), scheduling))

    @Test
    fun exposesAppVersionFromPackageMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val versionName = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName

        val viewModel = viewModel(context, RecordingScheduling())

        assertNotNull(versionName)
        assertTrue(versionName!!.matches(Regex("""\d+\.\d+\.\d+""")))
        assertEquals(versionName, viewModel.uiState.value.appVersion)
    }

    @Test
    fun changingEodTime_reschedulesTheWorker() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduling = RecordingScheduling()
        val viewModel = viewModel(context, scheduling)

        viewModel.setEodTime(21, 30)

        withTimeout(5_000) { scheduling.eodScheduled.await() }
    }

    @Test
    fun togglingNotifications_reschedulesTheWorker() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduling = RecordingScheduling()
        val viewModel = viewModel(context, scheduling)

        viewModel.setNotificationsEnabled(false)

        withTimeout(5_000) { scheduling.eodScheduled.await() }
    }
}
