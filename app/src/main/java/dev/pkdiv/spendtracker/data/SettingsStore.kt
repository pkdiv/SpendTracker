package dev.pkdiv.spendtracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val eodHour: Int = 23,
    val eodMinute: Int = 0,
    val lastEodDate: String? = null,
    val lastEomMonth: String? = null,
    val notificationsEnabled: Boolean = true,
    val smsDisclosureShown: Boolean = false,
    val notificationRationaleShown: Boolean = false,
    val lastSmsId: Long = 0,
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val EOD_HOUR = intPreferencesKey("eod_hour")
        val EOD_MINUTE = intPreferencesKey("eod_minute")
        val LAST_EOD_DATE = stringPreferencesKey("last_eod_date")
        val LAST_EOM_MONTH = stringPreferencesKey("last_eom_month")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SMS_DISCLOSURE_SHOWN = booleanPreferencesKey("sms_disclosure_shown")
        val NOTIFICATION_RATIONALE_SHOWN = booleanPreferencesKey("notification_rationale_shown")
        val LAST_SMS_ID = longPreferencesKey("last_sms_id")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            eodHour = prefs[Keys.EOD_HOUR] ?: 23,
            eodMinute = prefs[Keys.EOD_MINUTE] ?: 0,
            lastEodDate = prefs[Keys.LAST_EOD_DATE],
            lastEomMonth = prefs[Keys.LAST_EOM_MONTH],
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            smsDisclosureShown = prefs[Keys.SMS_DISCLOSURE_SHOWN] ?: false,
            notificationRationaleShown = prefs[Keys.NOTIFICATION_RATIONALE_SHOWN] ?: false,
            lastSmsId = prefs[Keys.LAST_SMS_ID] ?: 0L,
        )
    }

    suspend fun setEodTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.EOD_HOUR] = hour
            prefs[Keys.EOD_MINUTE] = minute
        }
    }

    suspend fun markEodRun(date: LocalDate) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LAST_EOD_DATE] = date.toString()
        }
    }

    suspend fun markEomRun(month: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LAST_EOM_MONTH] = month
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setSmsDisclosureShown(shown: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SMS_DISCLOSURE_SHOWN] = shown
        }
    }

    suspend fun setNotificationRationaleShown(shown: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_RATIONALE_SHOWN] = shown
        }
    }

    /** High-water mark for the incremental SMS inbox scan. */
    suspend fun setLastSmsId(smsId: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LAST_SMS_ID] = smsId
        }
    }
}
