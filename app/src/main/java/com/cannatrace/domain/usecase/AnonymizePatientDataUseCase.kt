package com.cannatrace.domain.usecase

import com.cannatrace.domain.repository.PrescriptionRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AnonymizePatientDataUseCase @Inject constructor(
    private val prescriptionRepository: PrescriptionRepository
) {

    companion object {
        private const val ANONYMIZED_VALUE = "ANONYMISE_RGPD"
        private val TWO_YEARS_MILLIS = TimeUnit.DAYS.toMillis(365L * 2)
    }

    /**
     * RGPD compliance: anonymizes patient data for prescriptions where
     * the last dispensation date is older than 2 years.
     * Returns the count of anonymized records.
     */
    suspend operator fun invoke(): Result<Int> {
        return try {
            val cutoffDate = System.currentTimeMillis() - TWO_YEARS_MILLIS
            val toAnonymize = prescriptionRepository.getPrescriptionsForAnonymization(cutoffDate)

            var count = 0
            for (prescription in toAnonymize) {
                if (!prescription.isAnonymized) {
                    prescriptionRepository.anonymizePrescription(prescription.id)
                    count++
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
