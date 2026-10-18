package dev.pkdiv.spendtracker.widget

/**
 * Requests a home-screen widget refresh. Abstracted so ingestion code can trigger a
 * refresh without depending on WorkManager directly (and so tests can pass a no-op).
 * The WorkManager-backed implementation is `WidgetScheduler`.
 */
interface WidgetRefresher {
    fun refresh()
}
