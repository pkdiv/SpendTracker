package dev.pkdiv.spendtracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.pkdiv.spendtracker.work.ReportScheduler
import javax.inject.Inject

@HiltAndroidApp
class SpendTrackerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var reportScheduler: ReportScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        reportScheduler.scheduleEod()
        reportScheduler.scheduleEom()
    }
}
