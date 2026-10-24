package dev.pkdiv.spendtracker.ui.disclosure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme

/**
 * Prominent disclosure rendered before any runtime permission prompt. The primary
 * action is affirmative consent for SMS access; declining keeps the app usable.
 */
@Composable
fun DisclosureScreen(
    onAllowSms: () -> Unit,
    onContinueWithoutSms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.disclosure_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.disclosure_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            DisclosureItem(
                icon = Icons.Filled.Sms,
                title = stringResource(R.string.disclosure_sms_title),
                body = stringResource(R.string.permission_sms_rationale),
            )

            Spacer(modifier = Modifier.height(20.dp))

            DisclosureItem(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.disclosure_notifications_title),
                body = stringResource(R.string.permission_notification_rationale),
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onAllowSms,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.disclosure_allow_sms))
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = onContinueWithoutSms,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.disclosure_continue_without_sms))
            }

            Text(
                text = stringResource(R.string.disclosure_without_sms_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Contextual explanation shown before the `POST_NOTIFICATIONS` system prompt. */
@Composable
fun NotificationRationaleDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_rationale_title)) },
        text = { Text(stringResource(R.string.notification_rationale_body)) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.notification_rationale_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notification_rationale_not_now))
            }
        },
    )
}

@Composable
private fun DisclosureItem(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Disclosure — light")
@Composable
private fun DisclosureScreenLightPreview() {
    SpendTrackerTheme(darkTheme = false) {
        DisclosureScreen(onAllowSms = {}, onContinueWithoutSms = {})
    }
}

@Preview(name = "Disclosure — dark")
@Composable
private fun DisclosureScreenDarkPreview() {
    SpendTrackerTheme(darkTheme = true) {
        DisclosureScreen(onAllowSms = {}, onContinueWithoutSms = {})
    }
}
