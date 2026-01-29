package com.sphere.shortvideos.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sphere.shortvideos.R
import com.sphere.shortvideos.helper.permission.PermissionHelper
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError
import java.util.concurrent.TimeUnit

/**
 * Date：2026/1/21
 * Describe: 通知助手类
 * 功能：注册用户解锁广播，间隔 30 分钟弹出通知
 */
object NotificationHelper {

    const val NOTIFICATION_ID_KEY = "notification_id"
    private const val WORK_NAME_23 = "local_notification_23"
    private const val WORK_NAME_59 = "local_notification_59"
    private const val WORK_NAME_79 = "local_notification_79"

    const val LOCAL_TYPE_23 = 23
    const val LOCAL_TYPE_59 = 59
    const val LOCAL_TYPE_79 = 79

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

    private val listLocal23Title = arrayListOf<Int>(
        R.string.notification_local_23_title1,
        R.string.notification_local_23_title2,
        R.string.notification_local_23_title3,
        R.string.notification_local_23_title4,
    )
    private val listLocal23Content = arrayListOf<Int>(
        R.string.notification_local_23_desc1,
        R.string.notification_local_23_desc2,
        R.string.notification_local_23_desc3,
        R.string.notification_local_23_desc4,
    )

    private val listLocal59Title = arrayListOf<Int>(
        R.string.notification_local_59_title1,
        R.string.notification_local_59_title2,
        R.string.notification_local_59_title3,
        R.string.notification_local_59_title4,
    )
    private val listLocal59Content = arrayListOf<Int>(
        R.string.notification_local_59_desc1,
        R.string.notification_local_59_desc2,
        R.string.notification_local_59_desc3,
        R.string.notification_local_59_desc4,
    )

    private val listLocal79Title = arrayListOf<Int>(
        R.string.notification_local_79_title1,
        R.string.notification_local_79_title2,
        R.string.notification_local_79_title3,
        R.string.notification_local_79_title4,
    )
    private val listLocal79Content = arrayListOf<Int>(
        R.string.notification_local_79_desc1,
        R.string.notification_local_79_desc2,
        R.string.notification_local_79_desc3,
        R.string.notification_local_79_desc4,
    )

    const val CHANNEL_ID = "unlock_notification_channel"
    private const val CHANNEL_NAME = "notification_unlock"
    private val notificationImplUU by lazy { NotificationImpl(1002, listUserUnlockTitle, listUserUnlockContent) }
    private val notificationImplSU by lazy { NotificationImpl(1003, listScreenUnlockTitle, listScreenUnlockContent) }
    private var localIndex23 by MMKVData(0)
    private var localIndex59 by MMKVData(0)
    private var localIndex79 by MMKVData(0)

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
     * 本地定时通知（23/59/79分钟轮询）
     */
    fun scheduleLocalNotifications(context: Context) {
        initChannel(context)
        enqueueLocalWorker(context, WORK_NAME_23, LOCAL_TYPE_23, if (isDebugMode) 1 else 23)
        enqueueLocalWorker(context, WORK_NAME_59, LOCAL_TYPE_59, if (isDebugMode) 5 else 59)
        enqueueLocalWorker(context, WORK_NAME_79, LOCAL_TYPE_79, if (isDebugMode) 7 else 79)
    }

    fun showLocalNotification(context: Context, type: Int) {
        val (titles, descs, index, notificationId) = when (type) {
            LOCAL_TYPE_59 -> Quad(listLocal59Title, listLocal59Content, localIndex59, 1102)
            LOCAL_TYPE_79 -> Quad(listLocal79Title, listLocal79Content, localIndex79, 1103)
            else -> Quad(listLocal23Title, listLocal23Content, localIndex23, 1101)
        }
        if (titles.isEmpty() || descs.isEmpty()) return
        val safeIndex = index.coerceAtLeast(0) % titles.size
        val title = context.getString(titles[safeIndex])
        val desc = context.getString(descs[safeIndex])
        when (type) {
            LOCAL_TYPE_59 -> localIndex59 = (safeIndex + 1) % titles.size
            LOCAL_TYPE_79 -> localIndex79 = (safeIndex + 1) % titles.size
            else -> localIndex23 = (safeIndex + 1) % titles.size
        }
        NotificationImpl(notificationId, arrayListOf(), arrayListOf(), 0).showNotification(context, title, desc)
    }

    private fun enqueueLocalWorker(context: Context, workName: String, type: Int, intervalMin: Long) {
        val request =
            PeriodicWorkRequestBuilder<NotificationWorker>(intervalMin, TimeUnit.MINUTES).setInputData(workDataOf(
                NotificationWorker.KEY_TYPE to type)).setInitialDelay(intervalMin, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.UPDATE, request)
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

    private data class Quad(val titles: ArrayList<Int>,
                            val descs: ArrayList<Int>,
                            val index: Int,
                            val notificationId: Int)
}