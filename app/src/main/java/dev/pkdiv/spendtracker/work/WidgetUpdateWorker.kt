package dev.pkdiv.spendtracker.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.pkdiv.spendtracker.widget.SpendWidgetUpdater

/** One-shot render of the home-screen spend widget from the repository. */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updater: SpendWidgetUpdater,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        updater.refreshAll()
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK = "spend_widget_update"
    }
}
