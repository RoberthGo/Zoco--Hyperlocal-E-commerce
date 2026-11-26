package com.example.zoco.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.zoco.domain.sync.SyncScheduler
import java.util.concurrent.TimeUnit

/** Implementación de [SyncScheduler] con WorkManager: trabajo único que espera a tener red. */
class WorkManagerSyncScheduler(context: Context) : SyncScheduler {

    private val appContext = context.applicationContext

    override fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SyncPendingWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        // KEEP: si ya hay una sincronización encolada no se duplica
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "sync_pending"
    }
}
