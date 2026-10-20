package dev.pkdiv.spendtracker.export

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.TransactionSource
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pure CSV serialization of transactions. Only parsed fields are written — never the
 * raw message body, sender or subject — so an export never carries message content.
 */
object TransactionCsv {

    private val TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private val HEADER = listOf(
        "date",
        "direction",
        "amount",
        "merchant",
        "category",
        "account",
        "source",
    )

    fun format(transactions: List<TransactionEntity>, zone: ZoneId): String = buildString {
        appendLine(HEADER.joinToString(","))
        transactions
            .sortedBy { it.timestamp }
            .forEach { transaction ->
                appendLine(
                    listOf(
                        TIMESTAMP.format(transaction.timestamp.atZone(zone)),
                        transaction.direction.name,
                        transaction.amount.toPlainString(),
                        transaction.merchant,
                        transaction.category.name,
                        transaction.account.orEmpty(),
                        (transaction.source ?: TransactionSource.SMS).name,
                    ).joinToString(",") { escape(it) },
                )
            }
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
