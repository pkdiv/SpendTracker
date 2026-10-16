package dev.pkdiv.spendtracker.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.pkdiv.spendtracker.repository.TransactionRepository

/**
 * One-time, idempotent backfill that re-categorizes rows left as `OTHER` before
 * categorization was wired on insert. Delegates to the repository so it reuses the
 * exact insert-time precedence.
 */
@HiltWorker
class BackfillCategoriesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        transactionRepository.recategorizeOtherTransactions()
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK = "backfill_categories"
    }
}
