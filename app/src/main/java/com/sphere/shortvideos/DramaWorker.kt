package com.sphere.shortvideos

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sphere.shortvideos.notification.NotificationHelper
import java.util.concurrent.TimeUnit

/**
 * Date：2026/8/17
 * Describe: 周期拉起常驻服务
 */
class DramaWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): ListenableWorker.Result {
        NotificationHelper.showFcmService(context)
        return ListenableWorker.Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "Worker_helper"

        fun openMe(context: Context) {
            val request = PeriodicWorkRequestBuilder<DramaWorker>(16, TimeUnit.MINUTES)
                .setInitialDelay(15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
