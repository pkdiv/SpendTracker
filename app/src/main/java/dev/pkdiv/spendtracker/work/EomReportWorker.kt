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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first

@HiltWorker
class EomReportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reportGenerator: ReportGenerator,
    private val settingsStore: SettingsStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (LocalDate.now().dayOfMonth != 1) return Result.success()
        val priorMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1)
        val report = reportGenerator.monthlyReport(priorMonth)

        if (report.transactionCount > 0) {
            postNotification(report.totalSpend)
        }

        settingsStore.markEomRun(priorMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")))
        return Result.success()
    }

    private fun postNotification(total: java.math.BigDecimal) {
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
        val text = applicationContext.getString(R.string.reports_month_total) + ": ₹%.2f".format(Locale.US, total)
        val notification = NotificationCompat.Builder(applicationContext, EOM_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.reports_eom))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(EOM_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EOM_CHANNEL,
                applicationContext.getString(R.string.notification_channel_eom),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            applicationContext.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EOM_CHANNEL = "eom_reports"
        const val EOM_ID = 1002
        const val UNIQUE_WORK = "eom_report_work"
    }
}
