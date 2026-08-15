package dev.pkdiv.spendtracker.ingestion.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsProcessor: SmsProcessor,
) {
    private val backfillMutex = Mutex()

    suspend fun backfill(): Int = backfillMutex.withLock {
        withContext(Dispatchers.IO) {
            val uri = Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms.Inbox.ADDRESS,
                Telephony.Sms.Inbox.BODY,
                Telephony.Sms.Inbox.DATE,
            )
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
                ?: error("SMS inbox query returned null")
            cursor.use {
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)
                var processed = 0
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIdx) ?: continue
                    val body = cursor.getString(bodyIdx) ?: continue
                    val receivedAtMillis = cursor.getLong(dateIdx)
                    val rawMessageRef = "sms:$receivedAtMillis:${body.hashCode()}"
                    smsProcessor.process(sender, body, rawMessageRef, receivedAtMillis)
                    processed++
                }
                Log.d("SmsReader", "Backfill read $processed inbox messages")
                processed
            }
        }
    }
}
