package com.cannatrace.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cannatrace.domain.repository.PrescriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker RGPD : Anonymisation automatique des données patient.
 *
 * Conformément à l'Article 17 du RGPD et aux obligations françaises (CNIL) :
 * - Les données d'identification des patients sont effacées automatiquement
 *   2 ans après la dernière dispensation.
 * - Ce worker s'exécute une fois par jour.
 * - L'anonymisation est irréversible (remplacement de l'ID patient par un
 *   identifiant opaque ANONYMISE_RGPD_<uuid>).
 */
@HiltWorker
class AnonymizationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prescriptionRepository: PrescriptionRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val twoYearsAgo = System.currentTimeMillis() - TWO_YEARS_MS

            // Récupère les prescriptions éligibles à l'anonymisation
            val prescriptions = prescriptionRepository.getPrescriptionsForAnonymization(twoYearsAgo)

            var anonymizedCount = 0
            prescriptions.forEach { prescription ->
                prescriptionRepository.anonymizePrescription(prescription.id)
                    .onSuccess { anonymizedCount++ }
            }

            Result.success(
                workDataOf(
                    KEY_ANONYMIZED_COUNT to anonymizedCount,
                    KEY_RESULT to "Anonymisation RGPD : $anonymizedCount dossier(s) traité(s)"
                )
            )
        } catch (e: Exception) {
            Result.failure(
                workDataOf(KEY_ERROR to "Erreur anonymisation RGPD : ${e.message}")
            )
        }
    }

    companion object {
        const val WORK_NAME = "cannatrace_anonymization"
        const val KEY_ANONYMIZED_COUNT = "anonymized_count"
        const val KEY_RESULT = "result"
        const val KEY_ERROR = "error"

        // Durée de conservation RGPD : 2 ans après la dernière dispensation
        val TWO_YEARS_MS = TimeUnit.DAYS.toMillis(730)

        /**
         * Crée une requête d'anonymisation quotidienne.
         * S'exécute une fois par jour, de préférence la nuit.
         */
        fun dailyAnonymizationRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<AnonymizationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateDelayToMidnight(), TimeUnit.MILLISECONDS)
                .build()
        }

        private fun calculateDelayToMidnight(): Long {
            val cal = java.util.Calendar.getInstance()
            val now = cal.timeInMillis
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 2) // 2h du matin
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            return cal.timeInMillis - now
        }
    }
}
