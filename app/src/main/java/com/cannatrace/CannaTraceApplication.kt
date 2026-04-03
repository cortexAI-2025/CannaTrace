package com.cannatrace

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cannatrace.workers.AlertWorker
import com.cannatrace.workers.AnonymizationWorker
import com.cannatrace.workers.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CannaTraceApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Sync worker - every 15 minutes
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // Anonymization worker - daily
        val anonymizationRequest = PeriodicWorkRequestBuilder<AnonymizationWorker>(
            1, TimeUnit.DAYS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            AnonymizationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            anonymizationRequest
        )

        // Alert worker - every hour
        val alertRequest = PeriodicWorkRequestBuilder<AlertWorker>(
            1, TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            AlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            alertRequest
        )
    }
}
