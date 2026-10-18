package dev.pkdiv.spendtracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pkdiv.spendtracker.MainActivity
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.repository.TransactionRepository
import dev.pkdiv.spendtracker.ui.Formatters
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Reads the current month's spend from the same repository flow the Home screen uses
 * and pushes it to every active widget instance. Rendering lives here (not in the
 * provider) so it can run from a WorkManager job while the app process is gone.
 */
@Singleton
class SpendWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
) {
    private val appWidgetManager: AppWidgetManager
        get() = AppWidgetManager.getInstance(context)

    private val component: ComponentName
        get() = ComponentName(context, SpendWidgetProvider::class.java)

    /** Re-renders every active widget. No-op when none are placed. */
    suspend fun refreshAll(now: LocalDate = LocalDate.now()) {
        val ids = appWidgetManager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        val views = buildViews(currentSpend(now))
        ids.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }

    /** Month-to-date spend for the month containing [now]. */
    suspend fun currentSpend(now: LocalDate = LocalDate.now()): WidgetSpend {
        val month = YearMonth.from(now)
        val zone = ZoneId.systemDefault()
        val from = month.atDay(1).atStartOfDay(zone).toInstant()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        return transactionRepository.observeBetween(from, to)
            .map { transactions -> WidgetSpend.from(transactions) }
            .first()
    }

    private fun buildViews(spend: WidgetSpend): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_spend).apply {
            val amount = Formatters.amountParts(spend.totalSpend)
            setTextViewText(R.id.widget_amount, amount.whole)
            setTextViewText(R.id.widget_paise, amount.paise)
            setTextViewText(
                R.id.widget_caption,
                if (spend.transactionCount == 0) {
                    context.getString(R.string.widget_empty)
                } else {
                    context.getString(R.string.widget_transactions, spend.transactionCount)
                },
            )
            setOnClickPendingIntent(R.id.widget_root, openAppIntent())
        }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
