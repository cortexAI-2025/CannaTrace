package com.cannatrace.data.repository

import com.cannatrace.data.local.database.dao.PrescriptionDao
import com.cannatrace.data.local.database.entities.PrescriptionEntity
import com.cannatrace.data.remote.CannaTraceApi
import com.cannatrace.domain.model.Prescription
import com.cannatrace.domain.repository.PrescriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrescriptionRepositoryImpl @Inject constructor(
    private val prescriptionDao: PrescriptionDao,
    private val api: CannaTraceApi
) : PrescriptionRepository {

    override fun getAllPrescriptions(): Flow<List<Prescription>> =
        prescriptionDao.getAllPrescriptions().map { entities -> entities.map { it.toDomain() } }

    override fun getPrescriptionsByBatch(batchId: String): Flow<List<Prescription>> =
        prescriptionDao.getPrescriptionsByBatch(batchId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getPrescriptionsByPrescriber(prescriberId: String): Flow<List<Prescription>> =
        prescriptionDao.getPrescriptionsByPrescriber(prescriberId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getPrescriptionById(id: String): Prescription? =
        prescriptionDao.getPrescriptionById(id)?.toDomain()

    override suspend fun createPrescription(prescription: Prescription): Result<Prescription> = runCatching {
        prescriptionDao.insertPrescription(prescription.toEntity())
        prescription
    }

    override suspend fun updatePrescription(prescription: Prescription): Result<Prescription> = runCatching {
        prescriptionDao.updatePrescription(prescription.toEntity())
        prescription
    }

    /**
     * RGPD : Anonymise les données d'une prescription spécifique.
     */
    override suspend fun anonymizePrescription(prescriptionId: String): Result<Unit> = runCatching {
        prescriptionDao.anonymizePatientData(prescriptionId)
    }

    /**
     * Retourne les prescriptions expirées (endDate < cutoffDate).
     */
    override suspend fun getExpiredPrescriptions(cutoffDate: Long): List<Prescription> {
        return prescriptionDao.getPrescriptionsToAnonymize(cutoffDate).map { it.toDomain() }
    }

    /**
     * Retourne les prescriptions éligibles à l'anonymisation RGPD
     * (dernière dispensation > 2 ans).
     */
    override suspend fun getPrescriptionsForAnonymization(cutoffDate: Long): List<Prescription> {
        return prescriptionDao.getPrescriptionsToAnonymize(cutoffDate).map { it.toDomain() }
    }

    override suspend fun updateLastDispensationDate(
        prescriptionId: String,
        date: Long
    ): Result<Unit> = runCatching {
        val existing = prescriptionDao.getPrescriptionById(prescriptionId)
            ?: throw IllegalArgumentException("Prescription introuvable: $prescriptionId")
        prescriptionDao.updatePrescription(existing.copy(lastDispensationDate = date))
    }

    private fun PrescriptionEntity.toDomain() = Prescription(
        id = id,
        anonymizedPatientId = anonymizedPatientId,
        prescriberId = prescriberId,
        batchId = batchId,
        dosage = dosage,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        adverseEffects = adverseEffects,
        isAnonymized = isAnonymized,
        lastDispensationDate = lastDispensationDate,
        createdAt = createdAt
    )

    private fun Prescription.toEntity() = PrescriptionEntity(
        id = id,
        anonymizedPatientId = anonymizedPatientId,
        prescriberId = prescriberId,
        batchId = batchId,
        dosage = dosage,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        adverseEffects = adverseEffects,
        isAnonymized = isAnonymized,
        lastDispensationDate = lastDispensationDate,
        createdAt = createdAt
    )
}
