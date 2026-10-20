package dev.pkdiv.spendtracker.ui.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.export.ExportFileWriter
import dev.pkdiv.spendtracker.export.ExportPeriod
import dev.pkdiv.spendtracker.export.TransactionCsv
import dev.pkdiv.spendtracker.export.bounds
import dev.pkdiv.spendtracker.repository.TransactionRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ExportUiState {
    data object Loading : ExportUiState

    data class Content(
        val period: ExportPeriod,
        val transactionCount: Int,
        val exporting: Boolean = false,
    ) : ExportUiState

    /** The file is written and ready to hand to the share sheet. */
    data class Shareable(val uri: Uri, val fileName: String) : ExportUiState
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val fileWriter: ExportFileWriter,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private var selectedPeriod: ExportPeriod = ExportPeriod.THIS_MONTH

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Loading)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        selectPeriod(ExportPeriod.THIS_MONTH)
    }

    fun selectPeriod(period: ExportPeriod) {
        selectedPeriod = period
        _uiState.value = ExportUiState.Content(period = period, transactionCount = 0)
        viewModelScope.launch {
            val count = load(period).size
            val current = _uiState.value
            if (current is ExportUiState.Content && current.period == period) {
                _uiState.value = current.copy(transactionCount = count)
            }
        }
    }

    /** Writes the CSV only when the user taps Export; nothing happens implicitly. */
    fun export() {
        val current = _uiState.value as? ExportUiState.Content ?: return
        if (current.exporting) return

        _uiState.value = current.copy(exporting = true)
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val csv = TransactionCsv.format(load(current.period), zone)
                fileWriter.write(fileName(current.period), csv)
            }
            _uiState.value = ExportUiState.Shareable(file.uri, file.fileName)
        }
    }

    /** Called once the share sheet has been launched, to return to the form. */
    fun onShared() = selectPeriod(selectedPeriod)

    private suspend fun load(period: ExportPeriod): List<TransactionEntity> {
        val bounds = period.bounds(LocalDate.now(zone), zone)
        return if (bounds == null) {
            transactionRepository.observeAll().first()
        } else {
            transactionRepository.observeBetween(bounds.first, bounds.second).first()
        }
    }

    private fun fileName(period: ExportPeriod): String = when (period) {
        ExportPeriod.THIS_MONTH -> "spendtracker-this-month.csv"
        ExportPeriod.LAST_MONTH -> "spendtracker-last-month.csv"
        ExportPeriod.ALL_TIME -> "spendtracker-all.csv"
    }
}
