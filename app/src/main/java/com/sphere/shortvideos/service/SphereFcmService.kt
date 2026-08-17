package com.sphere.shortvideos.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.RemoteMessage.PRIORITY_HIGH
import com.sphere.shortvideos.notification.NotificationHelper

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
        if (message.priority == PRIORITY_HIGH) {
            NotificationHelper.showFcmService(this)
        }
    }
}