package dev.pkdiv.spendtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.ui.Formatters

private val CreditGreen = Color(0xFF2E7D32)
private val DebitRed = Color(0xFFC62828)

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    onCategoryChange: (TransactionCategory) -> Unit,
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    val isCredit = transaction.direction == TransactionDirection.CREDIT
    val borderColor = if (isCredit) CreditGreen else DebitRed
    val amountColor = if (isCredit) CreditGreen else DebitRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = transaction.rawSender ?: transaction.merchant,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                val amountText = if (transaction.direction == TransactionDirection.DEBIT) {
                    stringResource(R.string.amount_debit, Formatters.amount(transaction.amount))
                } else {
                    stringResource(R.string.amount_credit, Formatters.amount(transaction.amount))
                }
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = amountColor,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = transaction.rawBody ?: transaction.merchant,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showCategoryMenu = true }) {
                    Text(stringResource(Formatters.categoryLabelRes(transaction.category)))
                }
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                ) {
                    TransactionCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(stringResource(Formatters.categoryLabelRes(category))) },
                            onClick = {
                                showCategoryMenu = false
                                onCategoryChange(category)
                            },
                        )
                    }
                }
            }
        }
    }
}
