package com.cannatrace.data.local.database.dao

import androidx.room.*
import com.cannatrace.data.local.database.entities.PrescriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {

    @Query("SELECT * FROM prescriptions ORDER BY startDate DESC")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: String): PrescriptionEntity?

    @Query("SELECT * FROM prescriptions WHERE anonymizedPatientId = :patientId ORDER BY startDate DESC")
    fun getPrescriptionsByPatient(patientId: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE prescriberId = :prescriberId ORDER BY startDate DESC")
    fun getPrescriptionsByPrescriber(prescriberId: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE batchId = :batchId ORDER BY startDate DESC")
    fun getPrescriptionsByBatch(batchId: String): Flow<List<PrescriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity)

    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)

    @Delete
    suspend fun deletePrescription(prescription: PrescriptionEntity)

    /**
     * RGPD: Récupère les prescriptions dont la dernière dispensation date de plus de 2 ans.
     * Ces prescriptions doivent être anonymisées automatiquement.
     */
    @Query("SELECT * FROM prescriptions WHERE endDate < :cutoffTimestamp AND anonymizedPatientId NOT LIKE 'ANONYMISE_%'")
    suspend fun getPrescriptionsToAnonymize(cutoffTimestamp: Long): List<PrescriptionEntity>

    /**
     * Anonymise les données patient d'une prescription (conformité RGPD).
     */
    @Query("UPDATE prescriptions SET anonymizedPatientId = 'ANONYMISE_RGPD_' || id, adverseEffects = '' WHERE id = :prescriptionId")
    suspend fun anonymizePatientData(prescriptionId: String)
}
