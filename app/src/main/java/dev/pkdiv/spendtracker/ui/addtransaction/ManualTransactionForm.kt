package dev.pkdiv.spendtracker.ui.addtransaction

import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.util.AmountInput
import java.math.BigDecimal
import java.time.LocalDate

/** Editable state of the manual-entry form. */
data class ManualTransactionForm(
    val amount: String = "",
    val direction: TransactionDirection = TransactionDirection.DEBIT,
    val merchant: String = "",
    val category: TransactionCategory = TransactionCategory.OTHER,
    val account: String = "",
    val date: LocalDate = LocalDate.now(),
) {
    /** Positive amount at paise precision, or null when the input is unusable. */
    val amountValue: BigDecimal? get() = AmountInput.parse(amount)

    /** Trimmed merchant/payee the user entered. */
    val merchantValue: String get() = merchant.trim()

    fun errors(): ManualTransactionErrors = ManualTransactionErrors(
        amount = amountValue == null,
        merchant = merchantValue.isEmpty(),
    )
}

data class ManualTransactionErrors(
    val amount: Boolean = false,
    val merchant: Boolean = false,
)
