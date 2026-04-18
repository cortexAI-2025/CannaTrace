package com.cannatrace.domain.repository

import com.cannatrace.domain.model.Plant
import com.cannatrace.domain.model.PlantStatus
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    fun getAllPlants(): Flow<List<Plant>>
    fun getPlantsByBatch(batchId: String): Flow<List<Plant>>
    suspend fun getPlantById(id: String): Plant?
    suspend fun getPlantByQrCode(qrCode: String): Plant?
    suspend fun getPlantByRfidTag(rfidTag: String): Plant?
    suspend fun createPlant(plant: Plant): Result<Plant>
    suspend fun updatePlant(plant: Plant): Result<Plant>
    suspend fun updatePlantStatus(plantId: String, status: PlantStatus): Result<Unit>
    suspend fun countPlantsByBatch(batchId: String): Int
    suspend fun syncPlants(): Result<Unit>
}
