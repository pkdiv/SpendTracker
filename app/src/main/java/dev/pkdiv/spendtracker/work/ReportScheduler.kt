package dev.pkdiv.spendtracker.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.pkdiv.spendtracker.data.SettingsStore
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class ReportScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val settingsStore: SettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun scheduleEod() {
        scope.launch {
            val settings = settingsStore.settings.first()
            val now = LocalDateTime.now()
            val next = now
                .withHour(settings.eodHour)
                .withMinute(settings.eodMinute)
                .withSecond(0)
                .withNano(0)
            val delayMinutes = java.time.Duration.between(now, next).toMinutes().coerceAtLeast(1)

            val request = PeriodicWorkRequestBuilder<EodReportWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                EodReportWorker.UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }

    fun scheduleEom() {
        val request = PeriodicWorkRequestBuilder<EomReportWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            EomReportWorker.UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
