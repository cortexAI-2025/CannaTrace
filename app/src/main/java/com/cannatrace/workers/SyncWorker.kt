package com.cannatrace.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cannatrace.domain.repository.AuditLogRepository
import com.cannatrace.domain.repository.BatchRepository
import com.cannatrace.domain.repository.PlantRepository
import com.cannatrace.domain.repository.StockRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker de synchronisation hors-ligne / en-ligne.
 * Planifié par WorkManager, s'exécute automatiquement lorsque la connectivité est disponible.
 *
 * Synchronise dans l'ordre :
 * 1. Les lots (Batches)
 * 2. Les plantes
 * 3. Les mouvements de stock
 * 4. Les logs d'audit (le plus critique : intégrité réglementaire)
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val batchRepository: BatchRepository,
    private val plantRepository: PlantRepository,
    private val stockRepository: StockRepository,
    private val auditLogRepository: AuditLogRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            setProgress(workDataOf(KEY_PROGRESS to "Synchronisation des lots..."))
            batchRepository.syncBatches().getOrThrow()

            setProgress(workDataOf(KEY_PROGRESS to "Synchronisation des plantes..."))
            plantRepository.syncPlants().getOrThrow()

            setProgress(workDataOf(KEY_PROGRESS to "Synchronisation des stocks..."))
            stockRepository.syncStockEntries().getOrThrow()

            setProgress(workDataOf(KEY_PROGRESS to "Synchronisation des logs d'audit..."))
            auditLogRepository.syncAuditLogs().getOrThrow()

            Result.success(workDataOf(KEY_RESULT to "Synchronisation terminée avec succès"))
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(KEY_ERROR to "Échec après $MAX_RETRY_COUNT tentatives: ${e.message}")
                )
            }
        }
    }

    companion object {
        const val WORK_NAME = "cannatrace_sync"
        const val KEY_PROGRESS = "progress"
        const val KEY_RESULT = "result"
        const val KEY_ERROR = "error"
        private const val MAX_RETRY_COUNT = 3

        /**
         * Crée une requête de synchronisation périodique (toutes les 15 minutes si connecté).
         */
        fun periodicSyncRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        }

        /**
         * Crée une requête de synchronisation immédiate (ex: après modification offline).
         */
        fun immediateSyncRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    2000L,
                    TimeUnit.MILLISECONDS
                )
                .build()
        }
    }
}
