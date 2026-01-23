package com.sphere.shortvideos.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sphere.shortvideos.notification.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Date：2026/1/23
 * Describe:
 */
class GoSettingAndCheckActivity : AppCompatActivity() {
    private var isFirst = true

    companion object {
        var flag: Int = 0 // 用户开启通知权限
        fun fetchIntent(activity: Activity, flag: Int = 0) : Intent{
            this.flag = flag
            val intent = Intent(activity, GoSettingAndCheckActivity::class.java)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (flag) {
            0 -> {
                val settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                }
                startActivity(settingsIntent)
            }

            else -> {
                finish()
                return
            }
        }
        checkHaveGet()
    }

    override fun onResume() {
        super.onResume()
        if (isFirst) {
            isFirst = false
            return
        }
        finish()
    }

    private fun checkHaveGet() {
        lifecycleScope.launch {
            while (true) {
                when (flag) {
                    0 -> {
                        if (NotificationHelper.hasNotificationPermission(this@GoSettingAndCheckActivity)) {
                            startActivity(Intent(this@GoSettingAndCheckActivity, GoSettingAndCheckActivity::class.java))
                            break
                        }
                    }

                    else -> {
                        break
                    }
                }
                delay(300)
            }
        }
    }

}