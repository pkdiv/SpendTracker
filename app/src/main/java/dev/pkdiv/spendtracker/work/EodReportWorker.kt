package dev.pkdiv.spendtracker.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.pkdiv.spendtracker.MainActivity
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.data.AppSettings
import dev.pkdiv.spendtracker.data.SettingsStore
import dev.pkdiv.spendtracker.reports.DailyReport
import dev.pkdiv.spendtracker.reports.ReportGenerator
import dev.pkdiv.spendtracker.ui.Formatters
import java.time.LocalDate
import kotlinx.coroutines.flow.first

@HiltWorker
class EodReportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reportGenerator: ReportGenerator,
    private val settingsStore: SettingsStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val settings = settingsStore.settings.first()
        // A catch-up run and the periodic run must not both post for the same day.
        if (settings.lastEodDate == today.toString()) return Result.success()

        val report = reportGenerator.dailyReport(today)
        if (report.transactionCount > 0) {
            postNotification(report, settings)
        }

        settingsStore.markEodRun(today)
        return Result.success()
    }

    private fun postNotification(report: DailyReport, settings: AppSettings) {
        if (!settings.notificationsEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        createChannel()
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = applicationContext.getString(
            R.string.notification_eod_body,
            Formatters.formatInr(report.totalSpend),
        )
        val notification = NotificationCompat.Builder(applicationContext, EOD_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.reports_eod))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(EOD_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EOD_CHANNEL,
                applicationContext.getString(R.string.notification_channel_eod),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            applicationContext.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EOD_CHANNEL = "eod_reports"
        const val EOD_ID = 1001
        const val UNIQUE_WORK = "eod_report_work"
        const val CATCH_UP_WORK = "eod_report_catch_up"
    }
}
