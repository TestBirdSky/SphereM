package com.sphere.shortvideos.helper.permission

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.sphere.shortvideos.helper.mmkv.MMKVData

/**
 * Date：2026/1/21
 * Describe:
 */
object PermissionHelper {
    var showOpenNotifDialogFlag by MMKVData(0) // 显示自定义弹窗 50就证明首次通知弹窗没展示

    fun isShowNotificationDialogHome(context: Context): Boolean {
        if (showOpenNotifDialogFlag > 50) return false
        if (areNotificationsEnabled(context)) {
            showOpenNotifDialogFlag = 100
            return false
        }
        showOpenNotifDialogFlag = 55
        return true
    }


    fun isShowNotificationDialogAfterRv(context: Context): Boolean {
        if (showOpenNotifDialogFlag > 80) return false
        if (areNotificationsEnabled(context)) {
            showOpenNotifDialogFlag = 100
            return false
        }
        showOpenNotifDialogFlag = 90
        return true
    }



    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}