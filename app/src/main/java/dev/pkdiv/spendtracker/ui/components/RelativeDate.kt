package dev.pkdiv.spendtracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.pkdiv.spendtracker.ui.Formatters
import java.time.LocalDate

/** "Today" / "Yesterday", falling back to a compact "12 Oct" label. */
@Composable
fun relativeDateLabel(date: LocalDate): String {
    val res = Formatters.relativeDateRes(date)
    return if (res != null) stringResource(res) else Formatters.dateLabel(date)
}
