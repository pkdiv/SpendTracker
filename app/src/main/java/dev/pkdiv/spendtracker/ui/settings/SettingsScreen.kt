package dev.pkdiv.spendtracker.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.ui.components.SectionHeader

@Composable
fun SettingsScreen(
    onNavigateToMerchantCategories: () -> Unit,
    onNavigateToExport: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SectionHeader(title = stringResource(R.string.settings_section_reports))
        SettingsGroup {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_eod_time)) },
                supportingContent = {
                    Text("%02d:%02d".format(state.settings.eodHour, state.settings.eodMinute))
                },
                leadingContent = { SettingIcon(Icons.Filled.Schedule) },
                modifier = Modifier.clickable { showTimePicker = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_notifications)) },
                supportingContent = { Text(stringResource(R.string.settings_notifications_hint)) },
                leadingContent = { SettingIcon(Icons.Filled.Notifications) },
                trailingContent = {
                    Switch(
                        checked = state.settings.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    )
                },
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_categories))
        SettingsGroup {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_merchant_categories)) },
                supportingContent = { Text(stringResource(R.string.settings_merchant_categories_hint)) },
                leadingContent = { SettingIcon(Icons.Filled.Category) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable { onNavigateToMerchantCategories() },
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_data))
        SettingsGroup {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export)) },
                supportingContent = { Text(stringResource(R.string.settings_export_hint)) },
                leadingContent = { SettingIcon(Icons.Filled.FileDownload) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable { onNavigateToExport() },
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_permissions))
        SettingsGroup {
            PermissionItem(
                icon = Icons.Filled.Sms,
                title = stringResource(R.string.settings_sms_permission),
                granted = state.smsPermissionGranted,
                modifier = Modifier.clickable { openAppSettings(context) },
            )
            PermissionItem(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.settings_notification_permission),
                granted = state.notificationPermissionGranted,
                modifier = Modifier.clickable { openAppSettings(context) },
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_about))
        SettingsGroup {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy_policy)) },
                supportingContent = { Text(stringResource(R.string.settings_privacy_policy_hint)) },
                leadingContent = { SettingIcon(Icons.Filled.Description) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable {
                    openUrl(context, context.getString(R.string.privacy_policy_url))
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_app_version)) },
                leadingContent = { SettingIcon(Icons.Filled.Info) },
                trailingContent = {
                    Text(
                        text = state.appVersion ?: stringResource(R.string.settings_app_version_unknown),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = state.settings.eodHour,
            initialMinute = state.settings.eodMinute,
            onConfirm = { hour, minute ->
                viewModel.setEodTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { SettingIcon(icon) },
        modifier = modifier,
        trailingContent = {
            Text(
                text = stringResource(
                    if (granted) R.string.settings_permission_granted else R.string.settings_permission_denied,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text(stringResource(R.string.settings_time_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_time_cancel))
            }
        },
        title = { Text(stringResource(R.string.settings_eod_time)) },
        text = { TimePicker(state = pickerState) },
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
