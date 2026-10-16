package dev.pkdiv.spendtracker.testutil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

/**
 * Base class for ViewModel tests.
 *
 * Installs an [UnconfinedTestDispatcher] as `Dispatchers.Main` so `viewModelScope` work runs
 * eagerly, and cancels every [tracked][track] ViewModel's scope *before* resetting Main.
 *
 * Without that cancellation a ViewModel coroutine can still be resuming on `Dispatchers.Main`
 * from a background thread (a Room callback, for example) after the test body ends. That
 * dispatch then races the next test's `Dispatchers.setMain`, and `TestMainDispatcher` throws
 * `IllegalStateException` when it detects the concurrent read/write.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val viewModels = mutableListOf<ViewModel>()

    @Before
    fun installMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
    }

    @After
    fun resetMainDispatcher() {
        viewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    /** Registers [viewModel] so its scope is cancelled when the test finishes. */
    protected fun <T : ViewModel> track(viewModel: T): T = viewModel.also { viewModels += it }
}
