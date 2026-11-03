package com.sphere.shortvideos.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val type = inputData.getInt(KEY_TYPE, NotificationHelper.LOCAL_TYPE_23)
        NotificationHelper.showLocalNotification(applicationContext, type)?.join()
        return Result.success()
    }

    companion object {
        const val KEY_TYPE = "local_notify_type"
    }
}
