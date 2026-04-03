package com.cannatrace.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cannatrace.domain.usecase.QrScanResult
import com.cannatrace.domain.usecase.ScanQrCodeUseCase
import com.cannatrace.utils.QrCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrScannerState(
    val scannedValue: String? = null,
    val lastScanInfo: String? = null,
    val isScanning: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val scanQrCodeUseCase: ScanQrCodeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(QrScannerState())
    val state: StateFlow<QrScannerState> = _state.asStateFlow()

    private var lastScannedRaw = ""

    /**
     * Traite un QR code détecté par la caméra.
     * Évite les doublons consécutifs (anti-spam).
     */
    fun onBarcodeDetected(rawValue: String) {
        if (rawValue == lastScannedRaw || _state.value.isScanning) return
        lastScannedRaw = rawValue

        viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }

            when (val result = scanQrCodeUseCase(rawValue)) {
                is QrScanResult.BatchFound -> {
                    _state.update {
                        it.copy(
                            isScanning = false,
                            lastScanInfo = "Lot détecté : ${result.batch.batchNumber} (${result.batch.strainName})",
                            scannedValue = rawValue
                        )
                    }
                }
                is QrScanResult.PlantFound -> {
                    _state.update {
                        it.copy(
                            isScanning = false,
                            lastScanInfo = "Plante détectée : ${result.plant.qrCode} — Lot: ${result.plant.batchId}",
                            scannedValue = rawValue
                        )
                    }
                }
                is QrScanResult.UnknownCode -> {
                    _state.update {
                        it.copy(
                            isScanning = false,
                            lastScanInfo = "Code inconnu : ${result.code.take(40)}",
                            scannedValue = rawValue
                        )
                    }
                }
                is QrScanResult.Error -> {
                    _state.update {
                        it.copy(
                            isScanning = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearScan() {
        _state.update { it.copy(scannedValue = null) }
        lastScannedRaw = ""
    }
}
