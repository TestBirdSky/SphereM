package com.sphere.shortvideos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sphere.shortvideos.notification.NotificationHelper

/**
 * Date：2026/8/17
 * Describe: 非精确闹钟续约 + 开机恢复调度
 */
class SphereBroad : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        runCatching {
            when (intent?.action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    DramaWorker.openMe(appContext)
                    NotificationHelper.scheduleInexactAlarm(appContext, force = true)
                }

                else
                -> {
                    NotificationHelper.scheduleInexactAlarm(appContext, force = true)
                    NotificationHelper.showTime79(appContext)
                }
            }
        }
    }
}
