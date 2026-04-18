package com.cannatrace.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager

/**
 * Relance les workers WorkManager après un redémarrage de l'appareil.
 * Les tâches périodiques (sync, anonymisation RGPD, alertes) sont
 * ainsi reprises automatiquement sans intervention de l'utilisateur.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val workManager = WorkManager.getInstance(context)

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.periodicSyncRequest()
        )

        workManager.enqueueUniquePeriodicWork(
            AnonymizationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            AnonymizationWorker.dailyAnonymizationRequest()
        )

        workManager.enqueueUniquePeriodicWork(
            AlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            AlertWorker.periodicAlertRequest()
        )
    }
}
