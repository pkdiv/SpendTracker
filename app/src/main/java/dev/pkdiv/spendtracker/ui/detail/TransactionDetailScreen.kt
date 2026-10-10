package dev.pkdiv.spendtracker.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.repository.MessageUnavailableReason
import dev.pkdiv.spendtracker.repository.TransactionDetail
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.components.DetailSkeleton
import dev.pkdiv.spendtracker.ui.components.MessageUnavailableCard
import dev.pkdiv.spendtracker.ui.components.OriginalMessageCard
import dev.pkdiv.spendtracker.ui.components.SectionHeader
import dev.pkdiv.spendtracker.ui.components.PreviewData
import dev.pkdiv.spendtracker.ui.components.categoryColors
import dev.pkdiv.spendtracker.ui.theme.SpendTheme
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
                actions = {
                    val body = (state as? DetailUiState.Content)?.detail?.body
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.detail_more),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detail_copy_message)) },
                            enabled = body != null,
                            onClick = {
                                menuExpanded = false
                                body?.let { clipboard.setText(AnnotatedString(it)) }
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            DetailUiState.Loading -> DetailSkeleton(Modifier.padding(padding))

            DetailUiState.NotFound -> LaunchedEffect(Unit) { onBack() }

            is DetailUiState.Content -> DetailContent(
                detail = current.detail,
                modifier = Modifier.padding(padding),
            )

            is DetailUiState.Unavailable -> DetailContent(
                detail = current.detail,
                modifier = Modifier.padding(padding),
                unavailableReason = current.reason,
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: TransactionDetail,
    modifier: Modifier = Modifier,
    unavailableReason: MessageUnavailableReason? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SummaryHeader(detail)

        Spacer(modifier = Modifier.height(16.dp))

        ParsedFieldsSection(detail)

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = stringResource(R.string.detail_original_message))

        if (detail.body == null) {
            MessageUnavailableCard(reason = unavailableReason ?: MessageUnavailableReason.DELETED)
        } else {
            OriginalMessageCard(detail = detail)
        }
    }
}

@Composable
private fun SummaryHeader(detail: TransactionDetail) {
    val isCredit = detail.direction == TransactionDirection.CREDIT
    val amountColor = if (isCredit) SpendTheme.colors.income else SpendTheme.colors.expense
    val amountText = (if (isCredit) "+" else "−") + Formatters.formatInr(detail.amount)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = amountText,
            style = MaterialTheme.typography.displayMedium,
            color = amountColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = detail.merchant,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryChip(detail.category)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Formatters.dateTimeLabel(detail.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryChip(category: TransactionCategory) {
    val colors = categoryColors(category)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.container,
    ) {
        Text(
            text = stringResource(Formatters.categoryLabelRes(category)),
            style = MaterialTheme.typography.labelMedium,
            color = colors.content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ParsedFieldsSection(detail: TransactionDetail) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            if (detail.account != null) {
                FieldRow(
                    label = stringResource(R.string.detail_account),
                    value = "•••• ${detail.account}",
                )
            }
            FieldRow(
                label = stringResource(R.string.detail_reference),
                value = detail.reference,
            )
            FieldRow(
                label = stringResource(R.string.detail_source),
                value = sourceLabel(detail.source),
            )
            FieldRow(
                label = stringResource(R.string.detail_date),
                value = Formatters.dateTimeLabel(detail.timestamp),
            )
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun sourceLabel(source: TransactionSource): String = when (source) {
    TransactionSource.SMS -> stringResource(R.string.detail_source_sms)
    TransactionSource.EMAIL -> stringResource(R.string.detail_source_email)
}

@Preview(name = "Content SMS", showBackground = true)
@Preview(name = "Content SMS (dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailContentSmsPreview() {
    SpendTrackerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DetailContent(detail = PreviewData.smsDetail)
        }
    }
}

@Preview(name = "Content Email", showBackground = true)
@Preview(name = "Content Email (dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailContentEmailPreview() {
    SpendTrackerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DetailContent(detail = PreviewData.emailDetail)
        }
    }
}

@Preview(name = "Unavailable", showBackground = true)
@Preview(name = "Unavailable (dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailUnavailablePreview() {
    SpendTrackerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DetailContent(
                detail = PreviewData.smsDetail.copy(body = null),
                unavailableReason = MessageUnavailableReason.PERMISSION_REVOKED,
            )
        }
    }
}

@Preview(name = "Loading", showBackground = true)
@Preview(name = "Loading (dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailLoadingPreview() {
    SpendTrackerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DetailSkeleton()
        }
    }
}
