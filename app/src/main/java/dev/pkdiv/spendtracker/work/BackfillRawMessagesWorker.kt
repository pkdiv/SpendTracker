package dev.pkdiv.spendtracker.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackfillRawMessagesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val rawMessageBackfill: RawMessageBackfill,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val permitted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        return if (rawMessageBackfill.run(permitted)) Result.success() else Result.retry()
    }

    companion object {
        const val UNIQUE_WORK = "backfill_raw_messages"
    }
}
