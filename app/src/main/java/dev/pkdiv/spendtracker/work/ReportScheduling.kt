package dev.pkdiv.spendtracker.work

/** Scheduling seam so settings changes can reschedule reports without WorkManager in tests. */
interface ReportScheduling {
    fun scheduleEod()

    fun scheduleEom()

    fun catchUpMissedReports()
}
