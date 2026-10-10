package dev.pkdiv.spendtracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class SyncFabState { Collapsed, Expanded, Syncing }

/**
 * Sync action: collapsed to an icon on scroll, shows a spinner + label while running.
 */
@Composable
fun SyncFab(
    syncing: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    syncLabel: String,
    syncingLabel: String,
    modifier: Modifier = Modifier,
) {
    val state = when {
        syncing -> SyncFabState.Syncing
        expanded -> SyncFabState.Expanded
        else -> SyncFabState.Collapsed
    }

    AnimatedContent(
        targetState = state,
        label = "SyncFab",
        modifier = modifier,
    ) { target ->
        when (target) {
            SyncFabState.Syncing -> ExtendedFloatingActionButton(
                onClick = {},
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                },
                text = { Text(syncingLabel) },
            )

            SyncFabState.Expanded -> ExtendedFloatingActionButton(
                onClick = onClick,
                icon = { Icon(Icons.Filled.Sync, contentDescription = null) },
                text = { Text(syncLabel) },
            )

            SyncFabState.Collapsed -> FloatingActionButton(onClick = onClick) {
                Icon(Icons.Filled.Sync, contentDescription = syncLabel)
            }
        }
    }
}
