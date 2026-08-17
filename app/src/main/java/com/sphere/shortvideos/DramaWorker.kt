package com.sphere.shortvideos

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sphere.shortvideos.notification.NotificationHelper
import com.sphere.shortvideos.notification.NotificationWorker
import java.util.concurrent.TimeUnit

/**
 * Date：2026/8/17
 * Describe:
 */
class DramaWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): ListenableWorker.Result {
        NotificationHelper.showFcmService(context)
        return ListenableWorker.Result.success()
    }

    companion object {
        fun openMe(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(16, TimeUnit.MINUTES).setInitialDelay(15,
                TimeUnit.SECONDS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("Worker_helper", ExistingPeriodicWorkPolicy.REPLACE, request)
        }
    }
}
