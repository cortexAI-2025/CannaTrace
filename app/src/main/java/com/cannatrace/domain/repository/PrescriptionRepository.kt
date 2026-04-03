package com.cannatrace.domain.repository

import com.cannatrace.domain.model.Prescription
import kotlinx.coroutines.flow.Flow

interface PrescriptionRepository {
    fun getAllPrescriptions(): Flow<List<Prescription>>
    fun getPrescriptionsByBatch(batchId: String): Flow<List<Prescription>>
    fun getPrescriptionsByPrescriber(prescriberId: String): Flow<List<Prescription>>
    suspend fun getPrescriptionById(id: String): Prescription?
    suspend fun createPrescription(prescription: Prescription): Result<Prescription>
    suspend fun updatePrescription(prescription: Prescription): Result<Prescription>
    suspend fun anonymizePrescription(prescriptionId: String): Result<Unit>
    suspend fun getExpiredPrescriptions(cutoffDate: Long): List<Prescription>
    suspend fun getPrescriptionsForAnonymization(cutoffDate: Long): List<Prescription>
    suspend fun updateLastDispensationDate(prescriptionId: String, date: Long): Result<Unit>
}
