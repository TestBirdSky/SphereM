package com.sphere.shortvideos.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.sphere.shortvideos.notification.NotificationHelper

/**
 * Date：2026/1/29
 * Describe:
 */
class SphereService : Service() {

    companion object {
        var isOpenService = false
    }

    override fun onCreate() {
        super.onCreate()
        isOpenService = true
        NotificationHelper.initChannel(this)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!NotificationHelper.hasNotificationPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NotificationHelper.NOTI_ID_FIXED, NotificationHelper.createFixNotification(this))
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        isOpenService = false
        super.onDestroy()
    }


}