package dev.pkdiv.spendtracker.ui.unrecognized

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageEntity
import dev.pkdiv.spendtracker.ui.components.EmptyState
import dev.pkdiv.spendtracker.ui.components.relativeDateLabel
import dev.pkdiv.spendtracker.ui.theme.SpendTheme
import java.time.ZoneId

/** Keeps every collapsed alert card the same height; longer bodies expand on demand. */
private const val COLLAPSED_LINES = 3

@Composable
fun UnrecognizedScreen(viewModel: UnrecognizedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UnrecognizedContent(
        state = state,
        onDismiss = viewModel::dismiss,
        onClearAll = viewModel::clearAll,
    )
}

@Composable
fun UnrecognizedContent(
    state: UnrecognizedUiState,
    onDismiss: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.unrecognized_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (state.messages.isNotEmpty()) {
                TextButton(onClick = { showClearConfirm = true }) {
                    Text(stringResource(R.string.unrecognized_clear_all))
                }
            }
        }

        if (state.messages.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.unrecognized_empty),
                message = stringResource(R.string.unrecognized_empty_message),
                modifier = Modifier.padding(16.dp),
                icon = Icons.Filled.Notifications,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    AlertCard(
                        message = message,
                        onDismiss = { onDismiss(message.id) },
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.unrecognized_clear_title)) },
            text = { Text(stringResource(R.string.unrecognized_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAll()
                    },
                ) {
                    Text(stringResource(R.string.unrecognized_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.unrecognized_clear_cancel))
                }
            },
        )
    }
}

@Composable
private fun AlertCard(message: UnrecognizedMessageEntity, onDismiss: () -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val date = remember(message.receivedAt) { message.receivedAt.atZone(zone).toLocalDate() }
    var expanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .animateContentSize()
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = SpendTheme.colors.warning,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = relativeDateLabel(date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result -> hasOverflow = result.hasVisualOverflow },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasOverflow || expanded) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(stringResource(if (expanded) R.string.detail_show_less else R.string.detail_show_full))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_recognize))
                }
            }
        }
    }
}
