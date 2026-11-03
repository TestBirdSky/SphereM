package com.sphere.shortvideos.notification

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chartboost.sdk.impl.fa
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.LoadingActivity
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.isKRAndSam
import com.sphere.shortvideos.helper.isLikedOSDevice
import com.sphere.shortvideos.helper.isOnePlusDevice
import com.sphere.shortvideos.helper.isSamsungDevice
import com.sphere.shortvideos.helper.isXiaomiDevice
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.helper.permission.PermissionHelper
import com.sphere.shortvideos.helper.risk.RiskHelper
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.isInteractive
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import com.sphere.shortvideos.service.SphereService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Date：2026/1/21
 * Describe: 通知助手类
 * 功能：注册用户解锁广播，间隔 30 分钟弹出通知
 */
object NotificationHelper {
    private val mainScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob()) }
    private val notifPeriod = NotifPeriodImpl()
    private var lastShowNotifTime = 0L
    var isInApp = false
    internal const val NOTI_ID_UNLOCK = 1002
    internal const val NOTI_ID_UNLOCK2 = 1003
    internal const val NOTI_ID_TIMER = 1111
    internal const val NOTI_ID_TIMER2 = 1112
    internal const val NOTI_ID_TIMER3 = 1113
    internal const val NOTI_ID_BACK = 1114
    const val NOTI_ID_MEDIA = 18900
    const val NOTI_ID_FCM_DATA = 1221
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

    /**
     * 所有通知文案只在第一次使用时随机一次，保证 title 和 desc 始终成对。
     */
    private val allNotificationText by lazy {
        listOf(
            listUserUnlockTitle.zip(listUserUnlockContent),
            listScreenUnlockTitle.zip(listScreenUnlockContent),
            listLocal23Title.zip(listLocal23Content),
            listLocal59Title.zip(listLocal59Content),
            listLocal79Title.zip(listLocal79Content),
        ).flatten().shuffled()
    }
    private val listAllTitle by lazy {
        ArrayList(allNotificationText.map { it.first })
    }
    private val listAllContent by lazy {
        ArrayList(allNotificationText.map { it.second })
    }

    const val CHANNEL_ID = "ser_notification_channel"
    private const val CHANNEL_NAME = "notification_ser"
    const val CHANNEL_ID_LOCAL = "local_notification_channel"  // 本地定时通知渠道（用于弹窗显示）
    const val CHANNEL_ID_LOCAL_MAX = "local_notification_channel_max"  // 本地定时通知渠道（用于弹窗显示）
    private const val CHANNEL_NAME_LOCAL = "notification_local"
    private const val CHANNEL_NAME_LOCAL_MAX = "notification_local_max"
    const val CHANNEL_ID_MEDIA = "m_notification_channel"
    const val CHANNEL_ID_MEDIA_MAX = "m_notification_channel_max"
    private const val CHANNEL_NAME_MEDIA = "notification_m"
    private const val CHANNEL_NAME_MEDIA_MAX = "notification_m_max"
    private val notificationImplUU by lazy {
        NotificationImpl(NOTI_ID_UNLOCK, listUserUnlockTitle, listUserUnlockContent)
    }
    private val notificationImplSU by lazy {
        NotificationImpl(NOTI_ID_UNLOCK2, listScreenUnlockTitle, listScreenUnlockContent)
    }
    private var localIndex23 by MMKVData(0)
    private var localIndex59 by MMKVData(0)
    private var localIndex79 by MMKVData(0)
    private var allNotificationIndex by MMKVData(0)

    private var screenUnlockReceiver: BroadcastReceiver? = null
    private var isRegistered = false

    /**
     * 初始化通知渠道
     */
    fun initChannel(context: Context) { // 解锁通知渠道（静默）
        val importance =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM && isLikedOSDevice()) NotificationManagerCompat.IMPORTANCE_MIN else NotificationManagerCompat.IMPORTANCE_DEFAULT
        NotificationManagerCompat.from(context)
            .createNotificationChannel(NotificationChannelCompat.Builder(CHANNEL_ID, importance).setSound(null, null)
                .setLightsEnabled(false).setVibrationEnabled(false).setShowBadge(false).setName(CHANNEL_NAME).build())
    }

    fun openMediaInfo() {
        mainScope.launch {
            delay(2500)
            logError("showMediaNotification")
            while (isSamsungDevice() && isInApp) {
                delay(1500)
                if (hasNotificationPermission(mApp).not()) {
                    break
                }
            }
            showMediaNotification(
                mApp, mApp.getString(listAllTitle.random()),
                mApp.getString(listAllContent.random()),
            )
        }
    }

    fun initNotificationChannel(context: Context): String {
        val channelIdStr = CHANNEL_ID_LOCAL_MAX
        NotificationManagerCompat.from(context).createNotificationChannel(NotificationChannelCompat.Builder(
            CHANNEL_ID_LOCAL_MAX,
            NotificationManagerCompat.IMPORTANCE_MAX).setLightsEnabled(true).setVibrationEnabled(true)
            .setShowBadge(true).setName(CHANNEL_NAME_LOCAL_MAX).build())
        return channelIdStr
    }

    fun initMediaNotificationChannel(context: Context, isShowN: Boolean = true): String {
        var channelIdStr: String
        if (isShowN) {
            channelIdStr = CHANNEL_ID_MEDIA_MAX
            NotificationManagerCompat.from(context).createNotificationChannel(NotificationChannelCompat.Builder(
                CHANNEL_ID_MEDIA_MAX,
                NotificationManagerCompat.IMPORTANCE_MAX).setLightsEnabled(true).setVibrationEnabled(true)
                .setShowBadge(true).setName(CHANNEL_NAME_MEDIA_MAX).build())
        } else {
            channelIdStr = CHANNEL_ID_MEDIA
            NotificationManagerCompat.from(context).createNotificationChannel(NotificationChannelCompat.Builder(
                CHANNEL_ID_MEDIA,
                NotificationManagerCompat.IMPORTANCE_DEFAULT,
            ).setShowBadge(true).setLightsEnabled(false).setVibrationEnabled(false).setName(CHANNEL_NAME_MEDIA).build())
        }
        return channelIdStr
    }

    private var job: Job? = null

    /**
     * 注册解锁广播
     */
    fun registerScreenUnlockReceiver(context: Context) {
        if (isRegistered) return
        screenUnlockReceiver = object : BroadcastReceiver() {
            private var timeLast = 0L
            override fun onReceive(context: Context?, intent: Intent?) {
                logError("registerScreenUnlockReceiver-->${intent?.action}")
                context?.let {
                    when (intent?.action) {
                        Intent.ACTION_USER_PRESENT -> {
                            job?.cancel()
                            job = CoroutineScope(Dispatchers.Main).launch {
                                delay(1000)
                                notificationImplUU.showNotification(it)
                            }
                        }

                        Intent.ACTION_SCREEN_ON -> {
                            if (System.currentTimeMillis() - timeLast < 45_000) return
                            job?.cancel()
                            job = CoroutineScope(Dispatchers.Main).launch {
                                if (hasMediaNotification(mApp).not()) {
                                    timeLast = System.currentTimeMillis()
                                    showMediaNotification(mApp,
                                        title = mApp.getString(listAllTitle.random()),
                                        contextStr = mApp.getString(listAllContent.random()))
                                } else {
                                    delay(10_000)
                                    if (isInteractive()) {
                                        timeLast = System.currentTimeMillis()
                                        notificationImplSU.showNotification(it)
                                    }
                                }
                            }
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
        registerScreenUnlockReceiver(context)
        openMediaInfo()
        launcherScope(if (isDebugMode) 3 else 13, 79, {
            showLocalNotification(context, LOCAL_TYPE_79)
        })
        launcherScope(if (isDebugMode) 1 else 5, 23, {
            showLocalNotification(context, LOCAL_TYPE_23)
        })
        enqueueLocalWorker(context, WORK_NAME_23, LOCAL_TYPE_59, 59)
    }

    private val workScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private fun launcherScope(firstDelay: Int, time: Int, timeCall: () -> Unit) {
        workScope.launch {
            delay(firstDelay * 60_000L)
            while (true) {
                timeCall.invoke()
                delay(time * 60000L)
            }
        }
    }

    fun showLocalNotification(context: Context, type: Int, notifId: Int? = null): Job? {
        logError("showLocalNotification--->$type")
        val (titles, descs, index, notificationId) = when (type) {
            LOCAL_TYPE_23 -> Quad(listLocal23Title, listLocal23Content, localIndex23, notifId ?: NOTI_ID_TIMER)
            LOCAL_TYPE_59 -> Quad(listLocal59Title, listLocal59Content, localIndex59, notifId ?: NOTI_ID_TIMER2)
            LOCAL_TYPE_79 -> Quad(listLocal79Title, listLocal79Content, localIndex79, notifId ?: NOTI_ID_TIMER3)
            else -> Quad(listAllTitle, listAllContent, allNotificationIndex, notifId ?: NOTI_ID_TIMER)
        }
        if (titles.isEmpty() || descs.isEmpty()) return null
        val safeIndex = index.coerceAtLeast(0) % titles.size
        val title = context.getString(titles[safeIndex])
        val desc = context.getString(descs[safeIndex])
        when (type) {
            LOCAL_TYPE_23 -> localIndex23 = (safeIndex + 1) % titles.size
            LOCAL_TYPE_59 -> localIndex59 = (safeIndex + 1) % titles.size
            LOCAL_TYPE_79 -> localIndex79 = (safeIndex + 1) % titles.size
            else -> allNotificationIndex = (safeIndex + 1) % titles.size
        }
        return showUnifiedNotification(context, notificationId, title, desc)
    }

    private fun enqueueLocalWorker(context: Context, workName: String, type: Int, intervalMin: Long) {
        val request =
            PeriodicWorkRequestBuilder<NotificationWorker>(intervalMin / 2, TimeUnit.MINUTES).setInputData(workDataOf(
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
        localEvent("all_noti_t", hashMapOf("type" to getType(id)))
    }

    fun clickNotiEvent(id: Int): String {
        val type = getType(id)
        localEvent("all_noti_c", hashMapOf("type" to type))
        return type
    }

    private fun getType(id: Int): String {
        return when (id) {
            NOTI_ID_TIMER -> "noti_1"
            NOTI_ID_TIMER2 -> "noti_2"
            NOTI_ID_TIMER3 -> "noti_3"
            NOTI_ID_BACK -> "noti_back" // 后台
            NOTI_ID_UNLOCK, NOTI_ID_UNLOCK2 -> "unlock"
            NOTI_ID_FCM_DATA -> "fcm"
            NOTI_ID_FIXED -> "fixed"
            NOTI_ID_MEDIA -> "media"
            else -> "unKnown"
        }
    }

    fun showOrUpdateNotificationService(context: Context) {
        if (isKRAndSam()) return
        runCatching {
            if (hasNotificationPermission(context).not()) {
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context is Application) {
                updateFixedNotification(context)
                return
            }
            if (SphereService.isOpenService || NotificationHelper.isInApp.not()) {
                updateFixedNotification(context)
            } else {
                try {
                    ContextCompat.startForegroundService(context, Intent(context, SphereService::class.java))
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun showFcmDataNotification(context: Context, title: String, desc: String) {
        if (hasNotificationPermission(context).not()) {
            logError("no post permission")
            return
        }
        showUnifiedNotification(context, NOTI_ID_FCM_DATA, title, desc)
    }

    fun updateFixedNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).notify(NOTI_ID_FIXED, createFixNotification(context))
        } catch (t: Throwable) {
        }
    }

    fun createFixNotification(context: Context): Notification {
        initChannel(context)
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
                setOnClickPendingIntent(R.id.layout_root, pendingIntent)
                setOnClickPendingIntent(R.id.tv_withdraw, pendingIntent)
            }

            NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_app)
                .setGroupSummary(false).setGroup("Drama").setSound(null).setContentTitle(title).setContentText(desc)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(remoteView)
                .setCustomBigContentView(remoteView).setOngoing(true).setOnlyAlertOnce(true)
                .setWhen(System.currentTimeMillis() - 60_000 * 60 * 24).setShowWhen(false)
                .setContentIntent(pendingIntent).build()
        }
        return buildFixedNotification()
    }

    private var jobBack: Job? = null
    private val listDelayTime = arrayListOf(6000, 15_000, 26_000, 36_000)
    fun onBackShowNotif() {
        if (!notifPeriod.isShowNotif(NOTI_ID_BACK, hasNotificationPermission(mApp))) return
        jobBack?.cancel()
        jobBack = mainScope.launch {
            delay(listDelayTime.random().toLong())
            if (isInteractive().not()) return@launch
            if (isInApp) return@launch
            showLocalNotification(mApp, arrayListOf(LOCAL_TYPE_23, LOCAL_TYPE_59, LOCAL_TYPE_79).random(), NOTI_ID_BACK)
        }
    }

    fun showUnifiedNotification(context: Context, notificationId: Int, title: String, desc: String): Job {
        val appContext = context.applicationContext
        return mainScope.launch {
            runCatching {
                val hasPermission = hasNotificationPermission(appContext)
                showMediaNotificationNow(appContext, title, desc)
                if (!notifPeriod.isShowNotif(notificationId, hasPermission)) return@launch
                if (!hasPermission) return@launch
                val now = System.currentTimeMillis()
                if (lastShowNotifTime > 0L && now - lastShowNotifTime < 20_000L) return@launch
                val notification = createLocalNotification(appContext, notificationId, title, desc)
                if (postNotification(appContext, notificationId, notification)) {
                    lastShowNotifTime = now
                }
            }.onFailure { logError("local notification error--->$it") }
        }
    }

    private fun createLocalNotification(
        context: Context,
        notificationId: Int,
        title: String,
        desc: String,
    ): Notification {
        val channelId = initNotificationChannel(context)
        val smallView = RemoteViews(context.packageName, R.layout.layout_notification_small).apply {
            setTextViewText(R.id.tv_title, title)
        }
        val centerView = RemoteViews(context.packageName, R.layout.layout_notif_center).apply {
            setTextViewText(R.id.tv_title, title)
            setTextViewText(R.id.tv_des, desc)
        }
        val bigView = RemoteViews(context.packageName, R.layout.layout_notification_big).apply {
            setTextViewText(R.id.tv_des, desc)
            setTextViewText(R.id.tv_btn, context.getString(R.string.start))
        }
        val builder = NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_notification_app)
            .setContentTitle(title).setContentText(desc).setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(getPendingI(context, notificationId)).setGroupSummary(false).setGroup("Sphere")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isXiaomiDevice()) {
                builder.setCustomContentView(centerView).setCustomBigContentView(bigView)
            } else {
                builder.setCustomContentView(smallView).setCustomHeadsUpContentView(centerView)
                    .setCustomBigContentView(bigView)
            }
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            if (isXiaomiDevice() || isSamsungDevice() || isOnePlusDevice()) {
                builder.setCustomContentView(centerView).setCustomHeadsUpContentView(centerView)
                    .setCustomBigContentView(bigView)
            } else {
                builder.setCustomContentView(bigView).setCustomHeadsUpContentView(bigView)
                    .setCustomBigContentView(bigView)
            }
        }
        return builder.build()
    }

    fun showMediaNotification(context: Context, title: String, contextStr: String) {
        val appContext = context.applicationContext
        mainScope.launch {
            showMediaNotificationNow(appContext, title, contextStr)
        }
    }

    private fun showMediaNotificationNow(context: Context, title: String, desc: String) {
        runCatching {
            logError("showMediaNotificationNow $title")
            val hasPermission = hasNotificationPermission(context) && hasMediaNotification(context)
            if (!notifPeriod.isShowNotif(NOTI_ID_MEDIA, hasPermission)) return
            val channelId = initMediaNotificationChannel(context, isShowN = !isInApp)
            val pendingIntent = getPendingI(context, NOTI_ID_MEDIA)
            val builder = NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_notification_app)
                .setContentIntent(pendingIntent).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setLargeIcon(Icon.createWithResource(context, R.drawable.ic_notification_small_app_icon))
                .setContentTitle(title).setContentText(desc)
            if (RiskHelper.isDoLimit()) {
                builder.setOngoing(true)
            }
            runCatching {
                MediaNotificationReflection.setMediaStyleByReflection(context,
                    builder,
                    "MediaSessionSphere",
                    pendingIntent)
            }
            postNotification(context, NOTI_ID_MEDIA, builder.build().also {
                if (RiskHelper.isDoLimit()) {
                    it.flags = it.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
                }
            })
        }.onFailure { logError("media notification error--->$it") }
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(context: Context, notificationId: Int, notification: Notification): Boolean {
        val posted = runCatching {
            val systemId =
                if (notificationId != NOTI_ID_MEDIA && notificationId != NOTI_ID_FIXED && Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM && isLikedOSDevice()) 23980 else notificationId
            NotificationManagerCompat.from(context).notify(systemId, notification)
        }.onFailure { logError("post notification error--->$it") }.isSuccess
        if (posted) {
            notifPeriod.showEvent(notificationId)
            showNotiEvent(notificationId)
        }
        return posted
    }

    fun getPendingI(context: Context, id: Int): PendingIntent {
        val pendingIntent =
            PendingIntent.getActivity(context, Random.nextInt(), Intent(context, LoadingActivity::class.java).apply {
                putExtra(NOTIFICATION_ID_KEY, id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }, PendingIntent.FLAG_IMMUTABLE)
        return pendingIntent
    }

    fun hasMediaNotification(context: Context): Boolean {
        return runCatching {
            NotificationManagerCompat.from(context).activeNotifications.any { notification ->
                notification.id == NOTI_ID_MEDIA
            }
        }.getOrDefault(false)
    }

    fun showFcmService(context: Context) {
        runCatching {
            if (SphereService.isOpenService.not()) {
                try {
                    ContextCompat.startForegroundService(context, Intent(context, SphereService::class.java))
                } catch (_: Throwable) {
                }
            }
        }

    }

}
