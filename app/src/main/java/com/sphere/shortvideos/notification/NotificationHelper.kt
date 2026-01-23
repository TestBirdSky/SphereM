package com.sphere.shortvideos.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.sphere.shortvideos.R
import com.sphere.shortvideos.helper.permission.PermissionHelper
import com.sphere.shortvideos.logError

/**
 * Date：2026/1/21
 * Describe: 通知助手类
 * 功能：注册用户解锁广播，间隔 30 分钟弹出通知
 */
object NotificationHelper {

    private val listUserUnlockTitle = arrayListOf<Int>(R.string.notification_checkin_title,
        R.string.notification_drama_title,
        R.string.notification_reward_title,
        R.string.notification_almost_title)
    private val listUserUnlockContent = arrayListOf<Int>(R.string.notification_checkin_desc,
        R.string.notification_drama_desc,
        R.string.notification_reward_desc,
        R.string.notification_almost_desc)

    private val listScreenUnlockTitle = arrayListOf<Int>(
        R.string.notification_screen_on_title1,
        R.string.notification_screen_on_title2,
        R.string.notification_screen_on_title3,
        R.string.notification_screen_on_title4,
    )
    private val listScreenUnlockContent = arrayListOf<Int>(R.string.notification_screen_on_des1,
        R.string.notification_screen_on_des2,
        R.string.notification_screen_on_des3,
        R.string.notification_screen_on_des4)
     const val CHANNEL_ID = "unlock_notification_channel"
    private const val CHANNEL_NAME = "notification_unlock"
    private val notificationImplUU by lazy { NotificationImpl(1002, listUserUnlockTitle, listUserUnlockContent) }
    private val notificationImplSU by lazy { NotificationImpl(1003, listScreenUnlockTitle, listScreenUnlockContent) }

    private var screenUnlockReceiver: BroadcastReceiver? = null
    private var isRegistered = false

    /**
     * 初始化通知渠道
     */
    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notification_User_PRESENT"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 注册解锁广播
     */
    fun registerScreenUnlockReceiver(context: Context) {
        if (isRegistered) return
        initChannel(context)
        screenUnlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                logError("registerScreenUnlockReceiver-->${intent?.action}")
                context?.let {
                    when (intent?.action) {
                        Intent.ACTION_USER_PRESENT -> {
                            notificationImplUU.showNotification(it)
                        }

                        Intent.ACTION_SCREEN_ON -> {
                            notificationImplSU.showNotification(it)
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(screenUnlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(screenUnlockReceiver, filter)
            }
        }

        isRegistered = true
    }

    /**
     * 注销广播
     */
    fun unregisterReceiver(context: Context) {
        runCatching {
            screenUnlockReceiver?.let {
                context.unregisterReceiver(it)
                screenUnlockReceiver = null
                isRegistered = false
            }
        }
    }


    /**
     * 检查通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return PermissionHelper.areNotificationsEnabled(context)
    }
}