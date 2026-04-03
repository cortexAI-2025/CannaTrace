package com.cannatrace.data.local.database.dao

import androidx.room.*
import com.cannatrace.data.local.database.entities.PlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants ORDER BY plantedAt DESC")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE batchId = :batchId ORDER BY plantedAt DESC")
    fun getPlantsByBatch(batchId: String): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getPlantById(id: String): PlantEntity?

    @Query("SELECT * FROM plants WHERE qrCode = :qrCode LIMIT 1")
    suspend fun getPlantByQrCode(qrCode: String): PlantEntity?

    @Query("SELECT * FROM plants WHERE rfidTag = :rfidTag LIMIT 1")
    suspend fun getPlantByRfidTag(rfidTag: String): PlantEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlant(plant: PlantEntity)

    @Update
    suspend fun updatePlant(plant: PlantEntity)

    @Delete
    suspend fun deletePlant(plant: PlantEntity)

    @Query("UPDATE plants SET status = :status WHERE id = :plantId")
    suspend fun updatePlantStatus(plantId: String, status: String)

    @Query("SELECT COUNT(*) FROM plants WHERE batchId = :batchId")
    suspend fun countPlantsByBatch(batchId: String): Int

    @Query("SELECT COUNT(*) FROM plants WHERE batchId = :batchId AND status = :status")
    suspend fun countPlantsByBatchAndStatus(batchId: String, status: String): Int
}
