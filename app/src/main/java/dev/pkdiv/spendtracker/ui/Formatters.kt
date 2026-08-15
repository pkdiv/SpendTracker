package dev.pkdiv.spendtracker.ui

import dev.pkdiv.spendtracker.parsing.TransactionCategory
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object Formatters {
    private val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun amount(value: BigDecimal): String = currency.format(value)

    fun categoryLabel(category: TransactionCategory): String = category.name.lowercase()
        .replaceFirstChar { it.uppercase() }
}
