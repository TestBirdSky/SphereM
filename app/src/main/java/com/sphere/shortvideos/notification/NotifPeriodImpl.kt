package com.sphere.shortvideos.notification

import com.sphere.shortvideos.helper.mmkv.mmkvIns
import com.sphere.shortvideos.isInteractive
import com.sphere.shortvideos.logError
import java.util.Calendar

internal class NotifPeriodImpl(
    private val readLastShown: (String) -> Long = { mmkvIns.decodeLong(it, 0L) },
    private val writeLastShown: (String, Long) -> Unit = { key, time ->
        mmkvIns.encode(key, time)
        Unit
    },
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val isDeviceInteractive: () -> Boolean = ::isInteractive,
) {
    fun isShowNotif(notificationId: Int, hasNotificationPermission: Boolean): Boolean {
        val now = currentTimeMillis()
        if (isInteractive().not()) {
            if (isNightModel(now)) {
                logError("in night model")
                return false
            }
        }
        val period = when (notificationId) {
            NotificationHelper.NOTI_ID_TIMER -> 23 * ONE_MIN
            NotificationHelper.NOTI_ID_TIMER2 -> 59 * ONE_MIN
            NotificationHelper.NOTI_ID_TIMER3 -> 79 * ONE_MIN
            NotificationHelper.NOTI_ID_BACK -> 6 * ONE_MIN
            NotificationHelper.NOTI_ID_UNLOCK -> 10 * ONE_MIN
            NotificationHelper.NOTI_ID_UNLOCK2 -> 30 * ONE_MIN
            NotificationHelper.NOTI_ID_MEDIA -> if (hasNotificationPermission) 6 * ONE_MIN else ONE_MIN
            NotificationHelper.NOTI_ID_FIXED -> 30_000L
            else -> return true
        }
        val lastShown = readLastShown(key(notificationId))
        return lastShown <= 0L || now - lastShown >= period
    }

    private fun isNightModel(now: Long): Boolean {
        val hour = Calendar.getInstance().apply {
            timeInMillis = now
        }.get(Calendar.HOUR_OF_DAY)
        if (hour !in 6..<23) return true
        return false
    }

    fun showEvent(notificationId: Int) {
        writeLastShown(key(notificationId), currentTimeMillis())
    }

    private fun key(notificationId: Int) = "notification_last_shown_$notificationId"

    private companion object {
        const val ONE_MIN = 60_000L
    }
}
