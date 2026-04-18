package com.cannatrace.data.repository

import com.cannatrace.data.local.database.dao.PlantDao
import com.cannatrace.data.local.database.entities.PlantEntity
import com.cannatrace.data.remote.CannaTraceApi
import com.cannatrace.domain.model.Plant
import com.cannatrace.domain.model.PlantStatus
import com.cannatrace.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantRepositoryImpl @Inject constructor(
    private val plantDao: PlantDao,
    private val api: CannaTraceApi
) : PlantRepository {

    override fun getAllPlants(): Flow<List<Plant>> =
        plantDao.getAllPlants().map { entities -> entities.map { it.toDomain() } }

    override fun getPlantsByBatch(batchId: String): Flow<List<Plant>> =
        plantDao.getPlantsByBatch(batchId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPlantById(id: String): Plant? =
        plantDao.getPlantById(id)?.toDomain()

    override suspend fun getPlantByQrCode(qrCode: String): Plant? =
        plantDao.getPlantByQrCode(qrCode)?.toDomain()

    override suspend fun getPlantByRfidTag(rfidTag: String): Plant? =
        plantDao.getPlantByRfidTag(rfidTag)?.toDomain()

    override suspend fun createPlant(plant: Plant): Result<Plant> = runCatching {
        plantDao.insertPlant(plant.toEntity())
        plant
    }

    override suspend fun updatePlant(plant: Plant): Result<Plant> = runCatching {
        plantDao.updatePlant(plant.toEntity())
        plant
    }

    override suspend fun updatePlantStatus(plantId: String, status: PlantStatus): Result<Unit> = runCatching {
        plantDao.updatePlantStatus(plantId, status.name)
    }

    override suspend fun countPlantsByBatch(batchId: String): Int =
        plantDao.countPlantsByBatch(batchId)

    override suspend fun syncPlants(): Result<Unit> = runCatching {
        val response = api.getPlants()
        if (response.isSuccessful) {
            response.body()?.forEach { entity ->
                plantDao.insertPlant(entity)
            }
        }
    }

    private fun PlantEntity.toDomain() = Plant(
        id = id,
        qrCode = qrCode,
        rfidTag = rfidTag,
        batchId = batchId,
        locationId = locationId,
        strain = strain,
        plantedAt = plantedAt,
        status = PlantStatus.valueOf(status),
        notes = notes
    )

    private fun Plant.toEntity() = PlantEntity(
        id = id,
        qrCode = qrCode,
        rfidTag = rfidTag,
        batchId = batchId,
        locationId = locationId,
        strain = strain,
        plantedAt = plantedAt,
        status = status.name,
        notes = notes
    )
}
