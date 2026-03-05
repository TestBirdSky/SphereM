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
        // 确保通知渠道在启动前台服务之前已初始化
        NotificationHelper.initChannel(this)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            // Android 要求：通过 startForegroundService 启动后，必须在 5s 内调用 startForeground
            val notification = NotificationHelper.createFixNotification(this)
            startForeground(NotificationHelper.NOTI_ID_FIXED, notification)
            START_STICKY
        } catch (t: Throwable) {
            // 如果因为任何原因拉起前台失败，立刻停止服务，避免 RemoteServiceException 崩溃
            stopSelf(startId)
            START_NOT_STICKY
        }
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