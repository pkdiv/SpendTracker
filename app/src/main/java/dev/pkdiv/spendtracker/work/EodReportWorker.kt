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
import dev.pkdiv.spendtracker.data.SettingsStore
import dev.pkdiv.spendtracker.reports.ReportGenerator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
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
        val report = reportGenerator.dailyReport(today)

        if (report.transactionCount > 0) {
            postNotification(report.totalSpend, report.transactionCount)
        }

        settingsStore.markEodRun(today)
        return Result.success()
    }

    private fun postNotification(total: BigDecimal, count: Int) {
        val settings = kotlinx.coroutines.runBlocking { settingsStore.settings.first() }
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
        val text = applicationContext.getString(R.string.home_total_spend) + ": " + format(total)
        val notification = NotificationCompat.Builder(applicationContext, EOD_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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

    private fun format(amount: BigDecimal): String = "₹%.2f".format(Locale.US, amount)

    companion object {
        const val EOD_CHANNEL = "eod_reports"
        const val EOD_ID = 1001
        const val UNIQUE_WORK = "eod_report_work"
    }
}
