package dev.pkdiv.spendtracker.ingestion.sms

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsProcessor: SmsProcessor,
) {
    suspend fun backfill() = withContext(Dispatchers.IO) {
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            "_id",
        )
        runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                val idIdx = cursor.getColumnIndexOrThrow("_id")
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIdx) ?: continue
                    val body = cursor.getString(bodyIdx) ?: continue
                    val id = cursor.getLong(idIdx)
                    smsProcessor.process(sender, body, "sms:$id")
                }
            }
        }
    }
}
