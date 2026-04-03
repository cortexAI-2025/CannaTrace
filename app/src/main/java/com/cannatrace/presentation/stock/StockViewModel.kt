package com.cannatrace.presentation.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cannatrace.domain.model.DivergenceReport
import com.cannatrace.domain.model.MovementType
import com.cannatrace.domain.model.StockEntry
import com.cannatrace.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class StockState(
    val entries: List<StockEntry> = emptyList(),
    val lowStockBatchIds: List<String> = emptyList(),
    val pendingDivergences: List<DivergenceReport> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StockState(isLoading = true))
    val state: StateFlow<StockState> = _state.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            stockRepository.getAllStockEntries().collect { entries ->
                val lowStock = stockRepository.getLowStockBatches(threshold = 100.0)
                _state.update {
                    it.copy(
                        entries = entries,
                        lowStockBatchIds = lowStock,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun addStockEntry(
        batchId: String,
        movementType: MovementType,
        quantity: Double,
        unit: String,
        reason: String,
        operatorId: String = "user-001"
    ) {
        viewModelScope.launch {
            val entry = StockEntry(
                id = UUID.randomUUID().toString(),
                batchId = batchId,
                movementType = movementType,
                quantity = quantity,
                unit = unit,
                reason = reason,
                operatorId = operatorId,
                timestamp = System.currentTimeMillis()
            )
            stockRepository.createStockEntry(entry)
                .onFailure { error ->
                    _state.update { it.copy(error = error.message) }
                }
        }
    }
}
