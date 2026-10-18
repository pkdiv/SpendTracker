package dev.pkdiv.spendtracker.work

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.pkdiv.spendtracker.widget.WidgetRefresher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager-backed [WidgetRefresher]. The [ExistingWorkPolicy.KEEP] policy collapses
 * bursts — a multi-message inbox backfill requests a refresh per parsed SMS, but only
 * one pending job survives at a time.
 */
@Singleton
class WidgetScheduler @Inject constructor(
    private val workManager: WorkManager,
) : WidgetRefresher {

    override fun refresh() {
        workManager.enqueueUniqueWork(
            WidgetUpdateWorker.UNIQUE_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build(),
        )
    }
}
