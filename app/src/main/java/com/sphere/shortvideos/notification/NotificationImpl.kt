package com.sphere.shortvideos.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.LoadingActivity
import com.sphere.shortvideos.logError

/**
 * Date：2026/1/21
 * Describe:
 */
class NotificationImpl(val notificationId: Int = 1000,
                       val strTitle: ArrayList<Int>,
                       val strContext: ArrayList<Int>,
                       val period: Long = 60000 * 30) {
    private var index = 0
    private var lastNotificationTime = 0L

    fun showNotification(context: Context) {
        if (NotificationHelper.hasNotificationPermission(context).not()) {
            logError("no post permission")
            return
        }
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime >= period) {
            lastNotificationTime = currentTime
            show(context, context.getString(strTitle[index]), context.getString(strContext[index]))
            index++
            if (index == strContext.size) {
                index = 0
            }
        }
    }

    private fun show(context: Context, title: String, contextStr: String) {
        logError("show--->$title --$contextStr")
        val intent = Intent(context, LoadingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification =
            NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_app)
                .setContentTitle(title)
                .setContentText(contextStr)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent).setAutoCancel(true).build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: Exception) {
            logError("show error--->$e")
            e.printStackTrace()
        }
    }

}