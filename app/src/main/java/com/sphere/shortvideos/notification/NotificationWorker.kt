package com.sphere.shortvideos.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val type = inputData.getInt(KEY_TYPE, NotificationHelper.LOCAL_TYPE_23)
        NotificationHelper.showLocalNotification(applicationContext, type)
        return Result.success()
    }

    companion object {
        const val KEY_TYPE = "local_notify_type"
    }
}
