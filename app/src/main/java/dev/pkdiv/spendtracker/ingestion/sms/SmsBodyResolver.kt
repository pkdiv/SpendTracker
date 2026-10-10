package dev.pkdiv.spendtracker.ingestion.sms

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pkdiv.spendtracker.util.Privacy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResolvedSmsBody(val sender: String, val body: String, val truncated: Boolean)

/** Re-fetches an SMS body by provider row id; null when deleted/unavailable. */
interface SmsBodyResolver {
    suspend fun resolve(smsId: Long): ResolvedSmsBody?
}

@Singleton
class ContentResolverSmsBodyResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) : SmsBodyResolver {

    override suspend fun resolve(smsId: Long): ResolvedSmsBody? = withContext(Dispatchers.IO) {
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY)
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "_id = ?",
            arrayOf(smsId.toString()),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sender = cursor.getString(0) ?: return@use null
                val raw = cursor.getString(1) ?: return@use null
                val capped = Privacy.capBody(raw)
                return@use ResolvedSmsBody(sender, capped.text, capped.truncated)
            }
            null
        }
    }
}
