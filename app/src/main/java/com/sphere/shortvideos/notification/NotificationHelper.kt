package com.sphere.shortvideos.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.LoadingActivity
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.permission.PermissionHelper
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.service.SphereService
import java.util.concurrent.TimeUnit

/**
 * Date：2026/1/21
 * Describe: 通知助手类
 * 功能：注册用户解锁广播，间隔 30 分钟弹出通知
 */
object NotificationHelper {
    private const val NOTI_ID_UNLOCK = 1002
    private const val NOTI_ID_TIMER = 1111
    const val NOTI_ID_FCM_DATA = 1092
    const val NOTI_ID_FIXED = 10091

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
    private val notificationImplUU by lazy {
        NotificationImpl(NOTI_ID_UNLOCK, listUserUnlockTitle, listUserUnlockContent)
    }
    private val notificationImplSU by lazy {
        NotificationImpl(NOTI_ID_UNLOCK, listScreenUnlockTitle, listScreenUnlockContent)
    }
    private var localIndex23 by MMKVData(0)
    private var localIndex59 by MMKVData(0)
    private var localIndex79 by MMKVData(0)

    private var screenUnlockReceiver: BroadcastReceiver? = null
    private var isRegistered = false

    /**
     * 初始化通知渠道
     */
    fun initChannel(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannel(NotificationChannelCompat.Builder(CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT).setSound(null, null).setLightsEnabled(false)
            .setVibrationEnabled(false).setShowBadge(false).setName(CHANNEL_NAME).build())
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
            LOCAL_TYPE_59 -> Quad(listLocal59Title, listLocal59Content, localIndex59, NOTI_ID_TIMER)
            LOCAL_TYPE_79 -> Quad(listLocal79Title, listLocal79Content, localIndex79, NOTI_ID_TIMER)
            else -> Quad(listLocal23Title, listLocal23Content, localIndex23, NOTI_ID_TIMER)
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

    fun showNotiEvent(id: Int) {
        val type = when (id) {
            NOTI_ID_TIMER -> "noti"
            NOTI_ID_UNLOCK -> "unlock"
            NOTI_ID_FCM_DATA -> "fcm"
            NOTI_ID_FIXED -> "fixed"
            else -> "unKnown"
        }
        localEvent("all_noti_t", hashMapOf("type" to type))
    }

    fun clickNotiEvent(id: Int) {
        val type = when (id) {
            NOTI_ID_TIMER -> "noti"
            NOTI_ID_UNLOCK -> "unlock"
            NOTI_ID_FCM_DATA -> "fcm"
            NOTI_ID_FIXED -> "fixed"
            else -> "unKnown"
        }
        localEvent("all_noti_c", hashMapOf("type" to type))
    }

    fun showNotificationService(context: Context) {
        if (hasNotificationPermission(context).not()) {
            return
        }
        if (SphereService.isOpenService) {
            updateFixedNotification(context)
        } else {
            try {
                ContextCompat.startForegroundService(context, Intent(context, SphereService::class.java))
            } catch (_: Throwable) {

            }
        }
    }

    fun showFcmDataNotification(context: Context, title: String, desc: String) {
        if (hasNotificationPermission(context).not()) {
            logError("no post permission")
            return
        }
        initChannel(context)
        NotificationImpl(NOTI_ID_FCM_DATA, arrayListOf(), arrayListOf(), 0).showNotification(context, title, desc)
    }

    fun updateFixedNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTI_ID_FIXED, createFixNotification(context))
        }
    }

    fun createFixNotification(context: Context): Notification {
        fun buildFixedNotification() = context.run {
            val title =
                WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue().first)
            val desc = getString(R.string.wi_notification_tips)
            val pendingIntent = PendingIntent.getActivity(this,
                NotificationHelper.NOTI_ID_FIXED,
                Intent(this, LoadingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(NOTIFICATION_ID_KEY, NotificationHelper.NOTI_ID_FIXED)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val remoteView = RemoteViews(packageName, R.layout.layout_notification_fix).apply {
                setTextViewText(R.id.tv_title, title)
                setTextViewText(R.id.tv_des, desc)
                setTextViewText(R.id.tv_withdraw, getString(R.string.withdraw))
                setOnClickPendingIntent(R.id.layout_root, pendingIntent)
                setOnClickPendingIntent(R.id.tv_withdraw, pendingIntent)
            }

            NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_app)
                .setGroupSummary(false).setSound(null).setGroup("Sphere").setContentTitle(title + desc)
                .setContentText(title + desc).setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(remoteView).setCustomBigContentView(remoteView).setOngoing(true)
                .setOnlyAlertOnce(true).setContentIntent(pendingIntent).build()
        }
        return buildFixedNotification()
    }
}