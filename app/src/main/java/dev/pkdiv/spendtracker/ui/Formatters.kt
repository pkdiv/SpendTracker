package dev.pkdiv.spendtracker.ui

import androidx.annotation.StringRes
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object Formatters {
    private val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun amount(value: BigDecimal): String = currency.format(value)

    @StringRes
    fun categoryLabelRes(category: TransactionCategory): Int = when (category) {
        TransactionCategory.FOOD -> R.string.category_food
        TransactionCategory.TRANSPORT -> R.string.category_transport
        TransactionCategory.SHOPPING -> R.string.category_shopping
        TransactionCategory.BILLS -> R.string.category_bills
        TransactionCategory.ENTERTAINMENT -> R.string.category_entertainment
        TransactionCategory.HEALTH -> R.string.category_health
        TransactionCategory.OTHER -> R.string.category_other
    }
}
