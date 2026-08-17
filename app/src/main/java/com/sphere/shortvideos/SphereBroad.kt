package com.sphere.shortvideos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sphere.shortvideos.notification.NotificationHelper

/**
 * Date：2026/8/17
 * Describe:
 */
class SphereBroad : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        runCatching {
            NotificationHelper.scheduleInexactAlarm()
            p0?.let {
                NotificationHelper.showTime79(p0)
            }
        }
    }
}