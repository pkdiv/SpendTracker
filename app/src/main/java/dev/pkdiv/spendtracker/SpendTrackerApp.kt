package dev.pkdiv.spendtracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import dev.pkdiv.spendtracker.work.BackfillRawMessagesWorker
import dev.pkdiv.spendtracker.work.ReportScheduler
import javax.inject.Inject

@HiltAndroidApp
class SpendTrackerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var reportScheduler: ReportScheduler

    @Inject lateinit var workManager: WorkManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        reportScheduler.scheduleEod()
        reportScheduler.scheduleEom()
        scheduleRawMessageBackfill()
    }

    private fun scheduleRawMessageBackfill() {
        val request = OneTimeWorkRequestBuilder<BackfillRawMessagesWorker>().build()
        workManager.enqueueUniqueWork(
            BackfillRawMessagesWorker.UNIQUE_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
