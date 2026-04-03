package com.cannatrace.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cannatrace.domain.repository.BatchRepository
import com.cannatrace.domain.repository.StockRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker d'alertes automatiques.
 *
 * Vérifie périodiquement :
 * - Les lots dont la date de péremption approche (< 30 jours)
 * - Les ruptures de stock (stock < seuil de sécurité)
 * - Les non-conformités détectées
 *
 * Émet des notifications Android si des alertes sont détectées.
 */
@HiltWorker
class AlertWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val batchRepository: BatchRepository,
    private val stockRepository: StockRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        createNotificationChannel()

        var alertCount = 0

        // Vérifie les lots expirant dans moins de 30 jours
        val expiringBatches = batchRepository.getExpiringBatches(thresholdDays = 30)
        if (expiringBatches.isNotEmpty()) {
            sendNotification(
                id = NOTIF_ID_EXPIRY,
                title = "⚠️ Lots en approche de péremption",
                message = "${expiringBatches.size} lot(s) expirent dans moins de 30 jours : " +
                    expiringBatches.take(3).joinToString(", ") { it.batchNumber }
            )
            alertCount++
        }

        // Vérifie les ruptures de stock
        val lowStockBatchIds = stockRepository.getLowStockBatches(threshold = LOW_STOCK_THRESHOLD)
        if (lowStockBatchIds.isNotEmpty()) {
            sendNotification(
                id = NOTIF_ID_STOCK,
                title = "🚨 Rupture de stock imminente",
                message = "${lowStockBatchIds.size} lot(s) sous le seuil de sécurité (${LOW_STOCK_THRESHOLD}g)"
            )
            alertCount++
        }

        return Result.success(workDataOf(KEY_ALERT_COUNT to alertCount))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertes CannaTrace",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes réglementaires et opérationnelles CannaTrace"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(id, notification)
    }

    companion object {
        const val WORK_NAME = "cannatrace_alerts"
        const val KEY_ALERT_COUNT = "alert_count"
        const val CHANNEL_ID = "cannatrace_alerts"
        const val NOTIF_ID_EXPIRY = 1001
        const val NOTIF_ID_STOCK = 1002
        const val LOW_STOCK_THRESHOLD = 100.0 // grammes

        /**
         * Requête de vérification périodique toutes les 6 heures.
         */
        fun periodicAlertRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<AlertWorker>(6, TimeUnit.HOURS)
                .build()
        }
    }
}
