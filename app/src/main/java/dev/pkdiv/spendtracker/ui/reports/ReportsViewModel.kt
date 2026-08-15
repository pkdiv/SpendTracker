package dev.pkdiv.spendtracker.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.reports.MonthlyReport
import dev.pkdiv.spendtracker.reports.ReportGenerator
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportsUiState(
    val loading: Boolean = true,
    val report: MonthlyReport? = null,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportGenerator: ReportGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentMonth()
    }

    fun loadMonth(month: LocalDate) {
        viewModelScope.launch {
            _uiState.value = ReportsUiState(loading = true)
            val report = reportGenerator.monthlyReport(month)
            _uiState.value = ReportsUiState(loading = false, report = report)
        }
    }

    private fun loadCurrentMonth() {
        loadMonth(LocalDate.now().withDayOfMonth(1))
    }
}
