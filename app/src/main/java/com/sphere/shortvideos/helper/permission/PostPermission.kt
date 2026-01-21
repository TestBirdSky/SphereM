package com.sphere.shortvideos.helper.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.sphere.shortvideos.helper.mmkv.MMKVData

/**
 * Date：2026/1/21
 * Describe: 通知权限管理类
 */
class PostPermission(private val activity: ComponentActivity) {
    private var isFirstReqPost by MMKVData(true)

    private var onPermissionResult: ((Int) -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                PermissionHelper.reqNotificationPermissionStatus = 100
                onPermissionResult?.invoke(100)
            } else {
                PermissionHelper.reqNotificationPermissionStatus = 50
                onPermissionResult?.invoke(0)
            }
        }

    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(activity,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestPermission(onResult: (int: Int) -> Unit) {
        onPermissionResult = onResult
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isFirstReqPost) {
                isFirstReqPost = false
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                when {
                    checkPermission() -> {
                        PermissionHelper.reqNotificationPermissionStatus = 100
                        onResult(100)
                    }
                    activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> { // 第二次弹窗
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    else -> {
                        onResult(30)
                    }
                }
            }

        } else {
            onResult(100)
        }
    }


}