package dev.pkdiv.spendtracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.repository.TransactionDetail
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.util.Privacy

private const val COLLAPSED_LINES = 8

/** Original source message: sender, received time, subject, selectable masked body. */
@Composable
fun OriginalMessageCard(
    detail: TransactionDetail,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    val body = detail.body.orEmpty()
    val masked = remember(body) { Privacy.maskSensitive(body) }
    val hasSensitive = masked != body
    val display = if (revealed) body else masked

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = sourceIcon(detail.source),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = detail.sender ?: stringResource(R.string.detail_unknown_sender),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Formatters.dateTimeLabel(detail.receivedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (detail.subject != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.subject,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.animateContentSize()) {
                SelectionContainer {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result -> hasOverflow = result.hasVisualOverflow },
                    )
                }
            }

            if (hasSensitive) {
                TextButton(onClick = { revealed = !revealed }) {
                    Text(stringResource(if (revealed) R.string.detail_hide_code else R.string.detail_reveal_code))
                }
            }

            if (hasOverflow || expanded) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(stringResource(if (expanded) R.string.detail_show_less else R.string.detail_show_full))
                }
            }

            if (detail.bodyTruncated) {
                Text(
                    text = stringResource(R.string.detail_truncated),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun sourceIcon(source: TransactionSource): ImageVector = when (source) {
    TransactionSource.SMS -> Icons.Filled.Sms
    TransactionSource.EMAIL -> Icons.Filled.Email
    TransactionSource.MANUAL -> Icons.Filled.Edit
}
