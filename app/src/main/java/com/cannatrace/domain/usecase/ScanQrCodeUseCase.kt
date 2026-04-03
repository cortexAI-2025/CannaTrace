package com.cannatrace.domain.usecase

import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.Plant
import com.cannatrace.domain.repository.BatchRepository
import com.cannatrace.domain.repository.PlantRepository
import javax.inject.Inject

sealed class QrScanResult {
    data class PlantFound(val plant: Plant) : QrScanResult()
    data class BatchFound(val batch: Batch) : QrScanResult()
    data class UnknownCode(val code: String) : QrScanResult()
    data class Error(val message: String) : QrScanResult()
}

class ScanQrCodeUseCase @Inject constructor(
    private val plantRepository: PlantRepository,
    private val batchRepository: BatchRepository
) {

    suspend operator fun invoke(qrCodeData: String): QrScanResult {
        if (qrCodeData.isBlank()) {
            return QrScanResult.Error("Code QR vide ou invalide")
        }

        // Try to find a plant by QR code
        val plant = plantRepository.getPlantByQrCode(qrCodeData)
        if (plant != null) {
            return QrScanResult.PlantFound(plant)
        }

        // Try to find a batch by batch number embedded in the QR
        val batch = batchRepository.getBatchByNumber(qrCodeData)
        if (batch != null) {
            return QrScanResult.BatchFound(batch)
        }

        // Try to find batch by ID if it looks like a UUID
        if (qrCodeData.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))) {
            val batchById = batchRepository.getBatchById(qrCodeData)
            if (batchById != null) {
                return QrScanResult.BatchFound(batchById)
            }
        }

        return QrScanResult.UnknownCode(qrCodeData)
    }
}
