package dev.pkdiv.spendtracker.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.pkdiv.spendtracker.data.SettingsStore
import java.time.LocalDateTime
import java.time.YearMonth
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
) : ReportScheduling {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun scheduleEod() {
        scope.launch {
            val settings = settingsStore.settings.first()
            val now = LocalDateTime.now()
            val next = nextEodRun(now, settings.eodHour, settings.eodMinute)
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

    override fun scheduleEom() {
        val request = PeriodicWorkRequestBuilder<EomReportWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            EomReportWorker.UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Queues catch-up runs for reports the periodic work may have missed (Doze,
     * reboot, force-stop). The workers themselves guard against duplicate posts via
     * `lastEodDate` / `lastEomMonth`.
     */
    override fun catchUpMissedReports() {
        scope.launch {
            val settings = settingsStore.settings.first()
            val now = LocalDateTime.now()
            val today = now.toLocalDate()

            val eodTime = today.atTime(settings.eodHour, settings.eodMinute)
            if (!now.isBefore(eodTime) && settings.lastEodDate != today.toString()) {
                enqueueOneShot<EodReportWorker>(EodReportWorker.CATCH_UP_WORK)
            }

            val priorMonthKey = YearMonth.from(today).minusMonths(1).toString()
            if (settings.lastEomMonth != priorMonthKey) {
                enqueueOneShot<EomReportWorker>(EomReportWorker.CATCH_UP_WORK)
            }
        }
    }

    private inline fun <reified W : ListenableWorker> enqueueOneShot(uniqueName: String) {
        workManager.enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<W>().build(),
        )
    }
}

/**
 * The next occurrence of the configured EOD time. If today's time has already
 * passed, the next run is tomorrow (not one minute from now).
 */
internal fun nextEodRun(now: LocalDateTime, hour: Int, minute: Int): LocalDateTime {
    val today = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    return if (today.isBefore(now)) today.plusDays(1) else today
}
