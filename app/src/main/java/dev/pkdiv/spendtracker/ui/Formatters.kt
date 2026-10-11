package dev.pkdiv.spendtracker.ui

import androidx.annotation.StringRes
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {

    private const val UNKNOWN_MERCHANT = "Unknown"

    private val inLocale = Locale("en", "IN")
    private val currencySymbol: String = DecimalFormatSymbols(inLocale).currencySymbol
    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(inLocale)

    /** Indian digit grouping (1,23,456), whole rupees only. */
    private val wholeFormat: NumberFormat = NumberFormat.getNumberInstance(inLocale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())

    data class AmountParts(val whole: String, val paise: String)

    /** Full INR string with Indian grouping, e.g. "₹1,23,456.00". */
    fun formatInr(value: BigDecimal): String = currencyFormat.format(value)

    /**
     * Splits an INR amount so the paise portion can be rendered smaller,
     * e.g. whole = "₹1,23,456", paise = ".00".
     */
    fun amountParts(value: BigDecimal): AmountParts {
        val rounded = value.setScale(2, RoundingMode.HALF_UP)
        val whole = rounded.setScale(0, RoundingMode.DOWN)
        val paise = rounded.subtract(whole)
            .abs()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
        return AmountParts(
            whole = currencySymbol + wholeFormat.format(whole),
            paise = ".%02d".format(paise),
        )
    }

    /** Compact fallback date label, e.g. "12 Oct". */
    fun dateLabel(date: LocalDate): String = date.format(dateFormatter)

    /**
     * Card title for a transaction. When the merchant couldn't be parsed it is
     * stored as [UNKNOWN_MERCHANT]; in that case append the raw sender in
     * parentheses so the row still identifies who the message came from.
     */
    fun merchantLabel(merchant: String, sender: String?): String =
        if (merchant == UNKNOWN_MERCHANT) {
            sender?.let { "$merchant ($it)" } ?: merchant
        } else {
            merchant
        }

    /** Today / Yesterday as resources, or null to fall back to [dateLabel]. */
    @StringRes
    fun relativeDateRes(date: LocalDate): Int? = when (date) {
        LocalDate.now() -> R.string.date_today
        LocalDate.now().minusDays(1) -> R.string.date_yesterday
        else -> null
    }

    /** Month header label, e.g. "October 2026". */
    fun monthLabel(date: LocalDate): String = date.format(monthFormatter)

    /** Received-time label, e.g. "12 Oct, 14:30". */
    fun dateTimeLabel(instant: Instant): String =
        instant.atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

    @StringRes
    fun categoryLabelRes(category: TransactionCategory): Int = when (category) {
        TransactionCategory.FOOD -> R.string.category_food
        TransactionCategory.TRANSPORT -> R.string.category_transport
        TransactionCategory.SHOPPING -> R.string.category_shopping
        TransactionCategory.BILLS -> R.string.category_bills
        TransactionCategory.ENTERTAINMENT -> R.string.category_entertainment
        TransactionCategory.HEALTH -> R.string.category_health
        TransactionCategory.OTHER -> R.string.category_other
        TransactionCategory.SELF_TRANSFER -> R.string.category_self_transfer
    }
}
