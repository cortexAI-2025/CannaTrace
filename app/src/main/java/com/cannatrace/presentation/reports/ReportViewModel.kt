package com.cannatrace.presentation.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cannatrace.domain.repository.AuditLogRepository
import com.cannatrace.domain.repository.BatchRepository
import com.cannatrace.domain.repository.StockRepository
import com.cannatrace.utils.CsvExporter
import com.cannatrace.utils.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportState(
    val isGeneratingInventoryPdf: Boolean = false,
    val isGeneratingAuditPdf: Boolean = false,
    val isGeneratingStockReport: Boolean = false,
    val isVerifyingChain: Boolean = false,
    val exportedFile: String? = null,
    val chainVerificationResult: Boolean? = null,
    val error: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batchRepository: BatchRepository,
    private val stockRepository: StockRepository,
    private val auditLogRepository: AuditLogRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state.asStateFlow()

    fun generateInventoryPdf() {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingInventoryPdf = true, error = null) }
            runCatching {
                val batches = mutableListOf<com.cannatrace.domain.model.Batch>()
                batchRepository.getAllBatches().first().let { batches.addAll(it) }
                PdfExporter.generateInventoryReport(context, batches)
            }.onSuccess { file ->
                _state.update {
                    it.copy(
                        isGeneratingInventoryPdf = false,
                        exportedFile = file.absolutePath
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isGeneratingInventoryPdf = false, error = error.message) }
            }
        }
    }

    fun generateInventoryCsv() {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                val batches = batchRepository.getAllBatches().first()
                CsvExporter.exportBatches(context, batches)
            }.onSuccess { file ->
                _state.update { it.copy(exportedFile = file.absolutePath) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun generateAuditPdf() {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingAuditPdf = true, error = null) }
            runCatching {
                val logs = mutableListOf<com.cannatrace.domain.model.AuditLog>()
                auditLogRepository.getAllAuditLogs().first().let { logs.addAll(it) }
                val batches = batchRepository.getAllBatches().first()
                val firstBatch = batches.firstOrNull() ?: return@runCatching null
                PdfExporter.generateAuditTrailReport(context, firstBatch, logs)
            }.onSuccess { file ->
                _state.update { it.copy(isGeneratingAuditPdf = false, exportedFile = file?.absolutePath) }
            }.onFailure { error ->
                _state.update { it.copy(isGeneratingAuditPdf = false, error = error.message) }
            }
        }
    }

    fun generateAuditCsv() {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                val logs = auditLogRepository.getAllAuditLogs().first()
                CsvExporter.exportAuditLogs(context, logs)
            }.onSuccess { file ->
                _state.update { it.copy(exportedFile = file.absolutePath) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun generateStockCsv() {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingStockReport = true, error = null) }
            runCatching {
                val entries = stockRepository.getAllStockEntries().first()
                CsvExporter.exportStockEntries(context, entries)
            }.onSuccess { file ->
                _state.update {
                    it.copy(
                        isGeneratingStockReport = false,
                        exportedFile = file.absolutePath
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isGeneratingStockReport = false, error = error.message) }
            }
        }
    }

    fun verifyHashChain() {
        viewModelScope.launch {
            _state.update { it.copy(isVerifyingChain = true, chainVerificationResult = null) }
            val isValid = auditLogRepository.verifyHashChain()
            _state.update { it.copy(isVerifyingChain = false, chainVerificationResult = isValid) }
        }
    }
}
