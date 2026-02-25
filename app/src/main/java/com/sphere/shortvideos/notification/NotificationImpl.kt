package com.sphere.shortvideos.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DecoratedCustomViewStyle
import androidx.core.app.NotificationManagerCompat
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.LoadingActivity
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import com.sphere.shortvideos.notification.NotificationHelper.NOTIFICATION_ID_KEY
import com.sphere.shortvideos.notification.NotificationHelper.NOTI_ID_MEDIA
import com.sphere.shortvideos.notification.NotificationHelper.initNotificationChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextInt

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

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context) {
        if (strTitle.isEmpty() || strContext.isEmpty()) return
        val total = minOf(strTitle.size, strContext.size)
        if (total <= 0) return
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime >= period) {
            val safeIndex = ((index % total) + total) % total
            lastNotificationTime = currentTime
            val title = context.getString(strTitle[safeIndex])
            val contextStr = context.getString(strContext[safeIndex])
            if (NotificationHelper.hasNotificationPermission(context)) {
                show(context, title, contextStr)
            }
            showMediaNotification(context, title, contextStr)
            index = (safeIndex + 1) % total
        }
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, title: String, contextStr: String) {
        showMediaNotification(context, title, contextStr)
        if (NotificationHelper.hasNotificationPermission(context)) {
            show(context, title, contextStr)
        } else {
            logError("no post permission")
        }
    }

    @SuppressLint("MissingPermission")
    fun showMediaNotification(context: Context, title: String, contextStr: String) {
        val channelIdStr = initNotificationChannel(context,
            NotificationHelper.hasNotificationPermission(context).not() && NotificationHelper.isInApp.not())
        logError("showMediaNotification=$notificationId")
        CoroutineScope(Dispatchers.Main).launch { // tag最好修改下
            val mMediaSession = MediaSessionCompat(context, "MediaSessionSphere")
            mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            mMediaSession.isActive = true
            mMediaSession.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .build()
            )
            val style = androidx.media.app.NotificationCompat.MediaStyle() //.setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mMediaSession.sessionToken)
            val builder = NotificationCompat.Builder(context, channelIdStr)
                .setSmallIcon(R.drawable.ic_notification_app)
                .setStyle(style)
                .setContentIntent(getPendingI(context))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setLargeIcon(Icon.createWithResource(mApp, R.drawable.ic_notification_small_app_icon))
                .setContentTitle(title)
                .setContentText(contextStr)
            runCatching {
                NotificationManagerCompat.from(context).notify(NOTI_ID_MEDIA, builder.build())
                NotificationHelper.showNotiEvent(NOTI_ID_MEDIA)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun show(context: Context, title: String, contextStr: String) {
        logError("show--->$title --$contextStr")
        NotificationHelper.showNotiEvent(notificationId)
        val channelIdStr = initNotificationChannel(context)
        val smallView = RemoteViews(context.packageName, R.layout.layout_notification_small).apply {
            setTextViewText(R.id.tv_title, title)
        }
        val bigView = RemoteViews(context.packageName, R.layout.layout_notification_big).apply {
            setTextViewText(R.id.tv_des, contextStr)
            setTextViewText(R.id.tv_btn, context.getString(R.string.start))
        }
        val builder = NotificationCompat.Builder(context, channelIdStr)
            .setSmallIcon(R.drawable.ic_notification_app)
            .setContentTitle(title).setContentText(contextStr)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(getPendingI(context)).setGroupSummary(false).setGroup("Sphere")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setStyle(DecoratedCustomViewStyle()).setCustomBigContentView(bigView)
                .setCustomHeadsUpContentView(smallView).setCustomContentView(smallView)
        } else {
            val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
            if (isXiaomi) {
                builder.setCustomContentView(smallView).setCustomHeadsUpContentView(smallView)
                    .setCustomBigContentView(bigView)
                builder.setStyle(DecoratedCustomViewStyle())
            } else {
                builder.setCustomContentView(bigView).setCustomHeadsUpContentView(bigView)
                    .setCustomBigContentView(bigView)
            }
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            logError("show error--->$e")
            e.printStackTrace()
        }
    }

    private fun getPendingI(context: Context): PendingIntent {
        val pendingIntent =
            PendingIntent.getActivity(context, Random.nextInt(), Intent(context, LoadingActivity::class.java).apply {
                putExtra(NOTIFICATION_ID_KEY, notificationId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }, PendingIntent.FLAG_IMMUTABLE)
        return pendingIntent
    }

//    private fun getDelPendingI(context: Context): PendingIntent {
//        val pendingIntent =
//            PendingIntent.getBroadcast(context, Random.nextInt(), Intent(context, SphereBroadcast::class.java).apply {
//                action = "com.sphere.shortvideos.NOTIFICATION_DELETED"
//            }, PendingIntent.FLAG_IMMUTABLE)
//        return pendingIntent
//    }

}