package com.sphere.shortvideos.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.LoadingActivity
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.notification.NotificationHelper.NOTIFICATION_ID_KEY

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

    fun showNotification(context: Context, title: String, contextStr: String) {
        if (NotificationHelper.hasNotificationPermission(context).not()) {
            logError("no post permission")
            return
        }
        show(context, title, contextStr)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun show(context: Context, title: String, contextStr: String) {
        logError("show--->$title --$contextStr")
        val intent = Intent(context, LoadingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NOTIFICATION_ID_KEY, notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val smallView = RemoteViews(context.packageName, R.layout.layout_notification_small).apply {
            setTextViewText(R.id.tv_title, title)
            setTextViewText(R.id.tv_des, contextStr)
            setOnClickPendingIntent(R.id.layout_root, pendingIntent)
        }
        val bigView = RemoteViews(context.packageName, R.layout.layout_notification_big).apply {
            setTextViewText(R.id.tv_title, title)
            setTextViewText(R.id.tv_des, contextStr)
            setTextViewText(R.id.tv_btn, context.getString(R.string.start))
            setOnClickPendingIntent(R.id.layout_root, pendingIntent)
            setOnClickPendingIntent(R.id.tv_btn, pendingIntent)
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_app).setContentTitle(title).setContentText(contextStr)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(smallView)
            .setCustomBigContentView(bigView).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent).setAutoCancel(true).build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: Exception) {
            logError("show error--->$e")
            e.printStackTrace()
        }
    }

}