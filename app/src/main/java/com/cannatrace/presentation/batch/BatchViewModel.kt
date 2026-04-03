package com.cannatrace.presentation.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.BatchStatus
import com.cannatrace.domain.model.BatchStatus.GRAINE
import com.cannatrace.domain.model.CorrectionNote
import com.cannatrace.domain.usecase.CreateBatchUseCase
import com.cannatrace.domain.usecase.TransitionBatchStatusUseCase
import com.cannatrace.domain.repository.BatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val batchRepository: BatchRepository,
    private val createBatchUseCase: CreateBatchUseCase,
    private val transitionBatchStatusUseCase: TransitionBatchStatusUseCase
) : ViewModel() {

    private val _listState = MutableStateFlow(BatchListState())
    val listState: StateFlow<BatchListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(BatchDetailState())
    val detailState: StateFlow<BatchDetailState> = _detailState.asStateFlow()

    private val _createState = MutableStateFlow(CreateBatchState())
    val createState: StateFlow<CreateBatchState> = _createState.asStateFlow()

    init {
        loadAllBatches()
    }

    fun loadAllBatches() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true) }
            batchRepository.getAllBatches().collect { batches ->
                _listState.update { state ->
                    state.copy(
                        batches = batches,
                        filteredBatches = applyFilters(batches, state.searchQuery, state.selectedStatus),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadBatchDetail(batchId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            val batch = batchRepository.getBatchById(batchId)
            _detailState.update { it.copy(batch = batch, isLoading = false) }
            if (batch != null) {
                batchRepository.getCorrectionNotes(batchId).collect { notes ->
                    _detailState.update { it.copy(correctionNotes = notes) }
                }
            }
        }
    }

    fun filterByStatus(status: BatchStatus?) {
        _listState.update { state ->
            state.copy(
                selectedStatus = status,
                filteredBatches = applyFilters(state.batches, state.searchQuery, status)
            )
        }
    }

    fun search(query: String) {
        _listState.update { state ->
            state.copy(
                searchQuery = query,
                filteredBatches = applyFilters(state.batches, query, state.selectedStatus)
            )
        }
    }

    private fun applyFilters(
        batches: List<Batch>,
        query: String,
        status: BatchStatus?
    ): List<Batch> {
        return batches.filter { batch ->
            val matchesStatus = status == null || batch.status == status
            val matchesQuery = query.isBlank() ||
                batch.batchNumber.contains(query, ignoreCase = true) ||
                batch.strainName.contains(query, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    fun transitionStatus(
        batchId: String,
        newStatus: BatchStatus,
        operatorId: String,
        reason: String = ""
    ) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, showTransitionDialog = false) }
            transitionBatchStatusUseCase(batchId, newStatus, operatorId, reason)
                .onSuccess { batch ->
                    _detailState.update {
                        it.copy(
                            batch = batch,
                            isLoading = false,
                            successMessage = "Statut mis à jour : ${newStatus.displayName}"
                        )
                    }
                }
                .onFailure { error ->
                    _detailState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Erreur lors de la transition."
                        )
                    }
                }
        }
    }

    fun addCorrectionNote(
        batchId: String,
        operatorId: String,
        justification: String,
        correctionType: String
    ) {
        viewModelScope.launch {
            val note = CorrectionNote(
                id = UUID.randomUUID().toString(),
                batchId = batchId,
                operatorId = operatorId,
                justification = justification,
                correctionType = correctionType
            )
            batchRepository.addCorrectionNote(note)
                .onSuccess {
                    _detailState.update {
                        it.copy(
                            showCorrectionNoteDialog = false,
                            successMessage = "Note de correction ajoutée."
                        )
                    }
                }
                .onFailure { error ->
                    _detailState.update { it.copy(error = error.message) }
                }
        }
    }

    // ─── Create batch ───────────────────────────────────────────────────

    fun onBatchNumberChanged(v: String) = _createState.update { it.copy(batchNumber = v) }
    fun onStrainNameChanged(v: String) = _createState.update { it.copy(strainName = v) }
    fun onWeightChanged(v: String) = _createState.update { it.copy(weight = v) }
    fun onThcChanged(v: String) = _createState.update { it.copy(thcContent = v) }
    fun onCbdChanged(v: String) = _createState.update { it.copy(cbdContent = v) }
    fun onLocationChanged(v: String) = _createState.update { it.copy(locationId = v) }
    fun onNotesChanged(v: String) = _createState.update { it.copy(notes = v) }

    fun createBatch(operatorId: String) {
        val state = _createState.value
        if (state.batchNumber.isBlank()) {
            _createState.update { it.copy(error = "Le numéro de lot est obligatoire.") }
            return
        }
        if (state.strainName.isBlank()) {
            _createState.update { it.copy(error = "Le nom de la souche est obligatoire.") }
            return
        }

        viewModelScope.launch {
            _createState.update { it.copy(isLoading = true, error = null) }
            val newBatch = Batch(
                id = UUID.randomUUID().toString(),
                batchNumber = state.batchNumber,
                strainName = state.strainName,
                status = GRAINE,
                weight = state.weight.toDoubleOrNull() ?: 0.0,
                thcContent = state.thcContent.toDoubleOrNull() ?: 0.0,
                cbdContent = state.cbdContent.toDoubleOrNull() ?: 0.0,
                createdAt = System.currentTimeMillis(),
                ownerId = operatorId,
                locationId = state.locationId,
                notes = state.notes
            )
            createBatchUseCase(newBatch, operatorId)
                .onSuccess {
                    _createState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    _createState.update {
                        it.copy(isLoading = false, error = error.message ?: "Erreur création lot.")
                    }
                }
        }
    }

    fun showTransitionDialog() = _detailState.update { it.copy(showTransitionDialog = true) }
    fun hideTransitionDialog() = _detailState.update { it.copy(showTransitionDialog = false) }
    fun showCorrectionNoteDialog() = _detailState.update { it.copy(showCorrectionNoteDialog = true) }
    fun hideCorrectionNoteDialog() = _detailState.update { it.copy(showCorrectionNoteDialog = false) }
    fun clearMessages() = _detailState.update { it.copy(error = null, successMessage = null) }
}
