package dev.pkdiv.spendtracker.reports

import dev.pkdiv.spendtracker.data.db.TransactionDao
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal

data class CategorySpend(val category: TransactionCategory, val total: BigDecimal)

data class MerchantSpend(val merchant: String, val total: BigDecimal)

data class DailyReport(
    val totalSpend: BigDecimal,
    val transactionCount: Int,
    val top: List<MerchantSpend>,
    val categories: List<CategorySpend>,
)

data class MonthlyReport(
    val totalSpend: BigDecimal,
    val transactionCount: Int,
    val categories: List<CategorySpend>,
    val topMerchants: List<MerchantSpend>,
    val previousMonthTotal: BigDecimal,
)
