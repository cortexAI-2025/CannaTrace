package com.cannatrace.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.BatchStatus
import com.cannatrace.domain.repository.BatchRepository
import com.cannatrace.domain.repository.StockRepository
import com.cannatrace.domain.usecase.CheckStockAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val totalActiveBatches: Int = 0,
    val batchesByStatus: Map<BatchStatus, Int> = emptyMap(),
    val expiringBatches: List<Batch> = emptyList(),
    val lowStockBatchIds: List<String> = emptyList(),
    val totalStockGrams: Double = 0.0,
    val isLoading: Boolean = false,
    val alerts: List<String> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val batchRepository: BatchRepository,
    private val stockRepository: StockRepository,
    private val checkStockAlertsUseCase: CheckStockAlertsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState(isLoading = true))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            batchRepository.getAllBatches().collect { batches ->
                val active = batches.filter { !it.isClosed }
                val byStatus = BatchStatus.values().associateWith { status ->
                    batches.count { it.status == status }
                }

                val expiring = batchRepository.getExpiringBatches(thresholdDays = 30)
                val lowStock = stockRepository.getLowStockBatches(threshold = 100.0)

                val alerts = mutableListOf<String>()
                if (expiring.isNotEmpty()) {
                    alerts.add("⚠️ ${expiring.size} lot(s) expirent dans moins de 30 jours")
                }
                if (lowStock.isNotEmpty()) {
                    alerts.add("🚨 ${lowStock.size} lot(s) sous le seuil de stock minimum")
                }

                _state.update {
                    it.copy(
                        totalActiveBatches = active.size,
                        batchesByStatus = byStatus,
                        expiringBatches = expiring,
                        lowStockBatchIds = lowStock,
                        isLoading = false,
                        alerts = alerts
                    )
                }
            }
        }
    }
}
