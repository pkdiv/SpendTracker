package dev.pkdiv.spendtracker.ingestion.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pkdiv.spendtracker.data.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Incremental inbox scan: only SMS with an `_ID` above the persisted high-water
 * mark are read, so a launch or manual Sync never re-reads the whole inbox.
 */
@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsProcessor: SmsProcessor,
    private val settingsStore: SettingsStore,
) {
    private val backfillMutex = Mutex()

    suspend fun backfill(): Int = backfillMutex.withLock {
        withContext(Dispatchers.IO) {
            val afterId = settingsStore.settings.first().lastSmsId
            val window = SmsScanWindow(afterId)
            val projection = arrayOf(
                Telephony.Sms.Inbox._ID,
                Telephony.Sms.Inbox.ADDRESS,
                Telephony.Sms.Inbox.BODY,
                Telephony.Sms.Inbox.DATE,
            )
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                window.selection,
                window.selectionArgs,
                window.sortOrder,
            ) ?: error("SMS inbox query returned null")
            cursor.use {
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox._ID)
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)
                var processed = 0
                var highWaterMark = afterId
                while (cursor.moveToNext()) {
                    val smsId = cursor.getLong(idIdx)
                    val sender = cursor.getString(addressIdx) ?: continue
                    val body = cursor.getString(bodyIdx) ?: continue
                    val receivedAtMillis = cursor.getLong(dateIdx)
                    val rawMessageRef = "sms:$smsId"
                    smsProcessor.process(sender, body, rawMessageRef, receivedAtMillis, smsId)
                    highWaterMark = SmsScanWindow.nextHighWaterMark(highWaterMark, smsId)
                    processed++
                }
                settingsStore.setLastSmsId(highWaterMark)
                Log.d("SmsReader", "Backfill read $processed inbox messages")
                processed
            }
        }
    }
}

/** Selection window for the incremental SMS scan, kept pure so it can be unit tested. */
internal data class SmsScanWindow(val afterId: Long) {
    val selection: String get() = "${Telephony.Sms.Inbox._ID} > ?"

    val selectionArgs: Array<String> get() = arrayOf(afterId.toString())

    val sortOrder: String get() = "${Telephony.Sms.Inbox._ID} ASC"

    companion object {
        fun nextHighWaterMark(current: Long, smsId: Long): Long = maxOf(current, smsId)
    }
}
