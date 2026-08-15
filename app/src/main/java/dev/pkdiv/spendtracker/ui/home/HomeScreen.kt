package dev.pkdiv.spendtracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.ui.Formatters
import dev.pkdiv.spendtracker.ui.components.TransactionRow

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rescanState by viewModel.rescanState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_total_spend),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = Formatters.amount(state.totalSpend),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.home_transactions_today, state.transactions.size),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { viewModel.rescan() },
                enabled = rescanState !is RescanState.Running,
            ) {
                Text(stringResource(R.string.home_rescan))
            }
            if (rescanState is RescanState.Running) {
                CircularProgressIndicator()
            }
        }

        when (val current = rescanState) {
            is RescanState.Done -> Text(
                text = stringResource(R.string.home_rescan_done, current.processed),
                style = MaterialTheme.typography.bodySmall,
            )
            is RescanState.NoPermission -> Text(
                text = stringResource(R.string.home_rescan_no_permission),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            is RescanState.Error -> Text(
                text = stringResource(R.string.home_rescan_error, current.message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            else -> Unit
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.transactions.isEmpty()) {
            Text(
                text = stringResource(R.string.home_no_transactions),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        Text(
            text = stringResource(R.string.home_recent),
            style = MaterialTheme.typography.titleMedium,
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.transactions) { transaction ->
                TransactionRow(transaction)
            }
        }
    }
}
