package com.sphere.shortvideos.helper.permission

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.sphere.shortvideos.helper.mmkv.MMKVData
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Date：2026/1/21
 * Describe:
 */
object PermissionHelper {
    var reqNotificationPermissionStatus by MMKVData(0) // 显示自定义弹窗
    private var permissionPostDay by MMKVData("") // 当天

    fun checkCueDay(): Boolean {
        val day = SimpleDateFormat("yyyy-MM-dd").format(Date())
        if (permissionPostDay != day) {
            reqNotificationPermissionStatus = 50
            permissionPostDay = day
            return false
        }
        return true
    }

    fun openNotificationSettings(activity: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        }
        activity.startActivity(intent)
    }

}