package com.sphere.shortvideos

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sphere.shortvideos.notification.NotificationWorker
import com.sphere.shortvideos.service.SphereService
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Date：2026/11/2
 * Describe:
 */
class SpWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runCatching {
            delay(1500)
            if (SphereService.isOpenService.not()) {
                try {
                    ContextCompat.startForegroundService(context,
                        Intent(context, SphereService::class.java))
                } catch (_: Throwable) {
                }
            }
        }

        return Result.success()
    }

    companion object {
        fun start(context: Context) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            val request = OneTimeWorkRequestBuilder<SpWorker>().build()
            workManager.enqueueUniqueWork("VideoCoreWorker", ExistingWorkPolicy.REPLACE, request)
        }
    }

}