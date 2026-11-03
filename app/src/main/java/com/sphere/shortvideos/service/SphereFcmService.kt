package com.sphere.shortvideos.service

import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sphere.shortvideos.notification.NotificationHelper
import com.sphere.shortvideos.notification.NotificationHelper.updateFixedNotification

/**
 * Date：2026/1/29
 * Describe:
 */
class SphereFcmService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        runCatching {
            val data = message.data
            if (data.isEmpty()) return
            val title = data["title"] ?: data["t"] ?: ""
            val desc = data["body"] ?: data["desc"] ?: data["content"] ?: ""
            NotificationHelper.showFcmDataNotification(this, title, desc)
        }

        val priority = message.priority
        if (RemoteMessage.PRIORITY_HIGH == priority) {
            runCatching {
                if (SphereService.isOpenService.not()) {
                    try {
                        ContextCompat.startForegroundService(this@SphereFcmService,
                            Intent(this@SphereFcmService, SphereService::class.java))
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }
}