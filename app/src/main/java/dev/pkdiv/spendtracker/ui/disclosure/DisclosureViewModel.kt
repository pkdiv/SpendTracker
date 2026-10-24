package dev.pkdiv.spendtracker.ui.disclosure

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pkdiv.spendtracker.data.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Gates the first-run prominent disclosure and the contextual notification
 * rationale. Both are persisted so the user is asked once, not on every launch.
 */
@HiltViewModel
class DisclosureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    /** `null` while the persisted flag loads, then whether to show the disclosure. */
    private val _showDisclosure = MutableStateFlow<Boolean?>(null)
    val showDisclosure: StateFlow<Boolean?> = _showDisclosure.asStateFlow()

    private val _showNotificationRationale = MutableStateFlow(false)
    val showNotificationRationale: StateFlow<Boolean> = _showNotificationRationale.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            if (hasSmsPermission()) {
                if (!settings.smsDisclosureShown) {
                    settingsStore.setSmsDisclosureShown(true)
                }
                _showDisclosure.value = false
                if (needsNotificationPermission() && !settings.notificationRationaleShown) {
                    _showNotificationRationale.value = true
                }
            } else {
                _showDisclosure.value = !settings.smsDisclosureShown
            }
        }
    }

    fun onSmsDisclosureAccepted() {
        persistSmsDisclosureShown()
        _showDisclosure.value = false
    }

    fun onSmsDisclosureDeclined() {
        persistSmsDisclosureShown()
        _showDisclosure.value = false
    }

    fun onSmsPermissionResult(granted: Boolean) {
        if (!granted) return
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            if (needsNotificationPermission() && !settings.notificationRationaleShown) {
                _showNotificationRationale.value = true
            }
        }
    }

    fun onNotificationRationaleHandled() {
        _showNotificationRationale.value = false
        viewModelScope.launch { settingsStore.setNotificationRationaleShown(true) }
    }

    private fun persistSmsDisclosureShown() {
        viewModelScope.launch { settingsStore.setSmsDisclosureShown(true) }
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
}
