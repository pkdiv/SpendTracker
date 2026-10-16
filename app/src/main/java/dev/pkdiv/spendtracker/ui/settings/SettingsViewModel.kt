package dev.pkdiv.spendtracker.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pkdiv.spendtracker.data.AppSettings
import dev.pkdiv.spendtracker.data.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val smsPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val appVersion: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val appVersion: String? = resolveAppVersion()

    val uiState: StateFlow<SettingsUiState> = settingsStore.settings
        .map { settings ->
            SettingsUiState(
                settings = settings,
                smsPermissionGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECEIVE_SMS,
                ) == PackageManager.PERMISSION_GRANTED,
                notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED,
                appVersion = appVersion,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsUiState(appVersion = appVersion),
        )

    /** The app's `versionName` from the manifest, or null if the package can't be read. */
    @Suppress("DEPRECATION")
    private fun resolveAppVersion(): String? = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (error: PackageManager.NameNotFoundException) {
        null
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) }
    }

    fun setEodTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsStore.setEodTime(hour, minute) }
    }
}
