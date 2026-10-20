package dev.pkdiv.spendtracker.ui.export

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.export.ExportPeriod
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme

@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state) {
        val shareable = state as? ExportUiState.Shareable ?: return@LaunchedEffect
        context.startActivity(shareIntent(context, shareable))
        viewModel.onShared()
    }

    ExportContent(
        state = state,
        onPeriodChange = viewModel::selectPeriod,
        onExport = viewModel::export,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportContent(
    state: ExportUiState,
    onPeriodChange: (ExportPeriod) -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state as? ExportUiState.Content

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.export_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (content == null) {
            ExportSkeleton(Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.export_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.export_period),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExportPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = period == content.period,
                            onClick = { onPeriodChange(period) },
                            enabled = !content.exporting,
                            label = { Text(stringResource(periodLabelRes(period))) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (content.transactionCount == 0) {
                        stringResource(R.string.export_empty)
                    } else {
                        stringResource(R.string.export_count, content.transactionCount)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onExport,
                    enabled = content.transactionCount > 0 && !content.exporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    if (content.exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        stringResource(
                            if (content.exporting) R.string.export_exporting else R.string.export_action,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(72.dp))
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp))
        }
    }
}

private fun shareIntent(context: Context, file: ExportUiState.Shareable): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, file.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, context.getString(R.string.export_share_title))
}

@StringRes
private fun periodLabelRes(period: ExportPeriod): Int = when (period) {
    ExportPeriod.THIS_MONTH -> R.string.export_period_this_month
    ExportPeriod.LAST_MONTH -> R.string.export_period_last_month
    ExportPeriod.ALL_TIME -> R.string.export_period_all
}

@Preview(name = "Light content", showBackground = true)
@Preview(name = "Dark content", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExportContentPreview() {
    SpendTrackerTheme {
        ExportContent(
            state = ExportUiState.Content(ExportPeriod.THIS_MONTH, transactionCount = 12),
            onPeriodChange = {},
            onExport = {},
            onBack = {},
        )
    }
}

@Preview(name = "Light empty", showBackground = true)
@Preview(name = "Dark empty", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExportEmptyPreview() {
    SpendTrackerTheme {
        ExportContent(
            state = ExportUiState.Content(ExportPeriod.LAST_MONTH, transactionCount = 0),
            onPeriodChange = {},
            onExport = {},
            onBack = {},
        )
    }
}
