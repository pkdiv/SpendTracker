package dev.pkdiv.spendtracker.ui.components

import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import dev.pkdiv.spendtracker.reports.CategorySpend
import dev.pkdiv.spendtracker.reports.MerchantSpend
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Fake, deterministic-ish fixtures for @Preview coverage. */
object PreviewData {

    private val now: Instant = Instant.now()

    fun transaction(
        id: Long = 1,
        merchant: String = "Swiggy",
        amount: String = "450.00",
        direction: TransactionDirection = TransactionDirection.DEBIT,
        category: TransactionCategory = TransactionCategory.FOOD,
        daysAgo: Long = 0,
    ) = TransactionEntity(
        id = id,
        amount = BigDecimal(amount),
        direction = direction,
        merchant = merchant,
        account = "HDFC ••1234",
        category = category,
        timestamp = now.minus(daysAgo, ChronoUnit.DAYS),
        rawMessageRef = "ref-$id",
        rawSender = "HDFCBK",
        rawBody = "Rs.$amount debited from A/c ••1234 to $merchant",
    )

    val transactions: List<TransactionEntity> = listOf(
        transaction(1, "Swiggy", "450.00", category = TransactionCategory.FOOD, daysAgo = 0),
        transaction(2, "Uber", "182.50", category = TransactionCategory.TRANSPORT, daysAgo = 0),
        transaction(3, "Amazon", "2,340.00", category = TransactionCategory.SHOPPING, daysAgo = 1),
        transaction(4, "Airtel", "799.00", category = TransactionCategory.BILLS, daysAgo = 1),
        transaction(5, "Salary", "85,000.00", direction = TransactionDirection.CREDIT, category = TransactionCategory.OTHER, daysAgo = 2),
    )

    val categories: List<CategorySpend> = listOf(
        CategorySpend(TransactionCategory.FOOD, BigDecimal("4200.00")),
        CategorySpend(TransactionCategory.SHOPPING, BigDecimal("3100.00")),
        CategorySpend(TransactionCategory.TRANSPORT, BigDecimal("1850.00")),
        CategorySpend(TransactionCategory.BILLS, BigDecimal("1200.00")),
    )

    val merchants: List<MerchantSpend> = listOf(
        MerchantSpend("Amazon", BigDecimal("3100.00")),
        MerchantSpend("Swiggy", BigDecimal("2400.00")),
        MerchantSpend("Uber", BigDecimal("1850.00")),
    )

    /** Per-day spend for a sparkline (₹ values as plain strings). */
    val dailySpend: List<BigDecimal> = listOf(
        "120", "0", "340", "560", "0", "780", "430", "210", "0", "640",
        "320", "0", "910", "540", "1200", "300", "0", "480", "700", "260",
        "880", "410", "0", "560", "940",
    ).map { BigDecimal(it) }
}
