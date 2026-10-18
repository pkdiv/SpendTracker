package dev.pkdiv.spendtracker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Home-screen widget showing the current month's spend. The data read and render
 * happen in a WorkManager job ([dev.pkdiv.spendtracker.work.WidgetScheduler]) so the
 * widget reflects the same repository flow the Home screen uses, even when the app
 * process isn't alive.
 */
@AndroidEntryPoint
class SpendWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var widgetRefresher: WidgetRefresher

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        widgetRefresher.refresh()
    }
}
