package com.sphere.shortvideos.notification

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.inmobi.media.ob
import com.sphere.shortvideos.DramaWorker
import com.sphere.shortvideos.R
import com.sphere.shortvideos.SphereBroad
import com.sphere.shortvideos.activity.LoadingActivity
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.helper.mmkv.MMKVRepository.getFirstsCountry
import com.sphere.shortvideos.helper.permission.PermissionHelper
import com.sphere.shortvideos.helper.task.TaskHelper
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Date：2026/1/21
 * Describe: 通知助手类
 * 功能：注册用户解锁广播，间隔 30 分钟弹出通知
 */
object NotificationHelper {

    private val notifIdList = (12000..24000).shuffled().take(7)
    private var index = Random.nextInt(0, 6)
    fun getShowNotifId(): Int {
        if (index > notifIdList.size) {
            index = 0
        }
        val ind = index++ % 7
        return notifIdList[ind]
    }

    var isInApp = false
    private const val NOTI_ID_UNLOCK = 1002
    private const val NOTI_ID_UNLOCK2 = 1003
    private const val NOTI_ID_TIMER = 1111
    private const val NOTI_ID_TIMER2 = 1112
    private const val NOTI_ID_TIMER3 = 1113
    private const val NOTI_ID_BACK = 1114

    /** Home 键退后台：9–18 点每日签到提醒 */
    const val NOTI_ID_HOME_CHECKIN = 1115
    const val NOTI_ID_MEDIA = 18900
    const val NOTI_ID_FCM_DATA = 1221
    const val NOTI_ID_FIXED = 10091

    const val NOTIFICATION_ID_KEY = "notification_id"
    private const val WORK_NAME_59 = "drama_shape_notif_tips"

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

    const val CHANNEL_ID = "ser_notification_channel"
    private const val CHANNEL_NAME = "notification_drama"
    const val CHANNEL_ID_LOCAL = "local_notification_channel"  // 本地定时通知渠道（用于弹窗显示）
    const val CHANNEL_ID_LOCAL_MAX = "local_notification_channel_max"  // 本地定时通知渠道（用于弹窗显示）
    private const val CHANNEL_NAME_LOCAL = "sphere_drama_helper"
    private const val CHANNEL_NAME_LOCAL_MAX = "notification_sphere_drama"

    /** 媒体通知专用渠道（与普通本地通知分离） */
    const val CHANNEL_ID_MEDIA = "media_notification_channel"
    const val CHANNEL_ID_MEDIA_MAX = "media_notification_channel_max"
    private const val CHANNEL_NAME_MEDIA = "sphere_drama_media"
    private const val CHANNEL_NAME_MEDIA_MAX = "notification_sphere_media"
    private val notificationImplUU by lazy {
        NotificationImpl(NOTI_ID_UNLOCK,
            listUserUnlockTitle,
            listUserUnlockContent,
            period = if (isSamsungDevice()) 10 * 60_000L else 5 * 60_000L)
    }
    private val notificationImplSU by lazy {
        NotificationImpl(NOTI_ID_UNLOCK, listScreenUnlockTitle, listScreenUnlockContent)
    }
    private val emptyTitleList = arrayListOf<Int>()
    private val emptyDescList = arrayListOf<Int>()
    private val directImplCache = java.util.concurrent.ConcurrentHashMap<Int, NotificationImpl>()
    private fun directImpl(id: Int): NotificationImpl = directImplCache.getOrPut(id) {
        NotificationImpl(id, emptyTitleList, emptyDescList, 0)
    }

    private var localIndex23 by MMKVData(0)
    private var localIndex59 by MMKVData(0)
    private var localIndex79 by MMKVData(0)

    /** 首页签到 Home 通知最近触发的日历日 yyyy-MM-dd，每天最多一次 */
    private var lastHomeCheckInDay by MMKVData("")

    private var screenUnlockReceiver: BroadcastReceiver? = null
    private var isRegistered = false


    fun isGoogleDevice() = Build.MANUFACTURER.equals("Google", ignoreCase = true)
    fun isXiaomiDevice() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    fun isOnePlusDevice() =
        Build.MANUFACTURER.equals("OnePlus", ignoreCase = true) || Build.BRAND.equals("OnePlus", ignoreCase = true)

    fun isFCNTDevice(): Boolean = Build.MANUFACTURER.equals("fcnt", ignoreCase = true)

    fun isSharpDevice(): Boolean = Build.MANUFACTURER.equals("sharp", ignoreCase = true)

    fun isLikedOSDevice(): Boolean = isGoogleDevice() || isXiaomiDevice() || isFCNTDevice() || isSharpDevice()

    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true) || Build.BRAND.equals("samsung",
            ignoreCase = true)
    }


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

    fun initNotificationChannel(context: Context): String {
        val channelIdStr: String = CHANNEL_ID_LOCAL_MAX
        NotificationManagerCompat.from(context).createNotificationChannel(NotificationChannelCompat.Builder(
            CHANNEL_ID_LOCAL_MAX,
            NotificationManagerCompat.IMPORTANCE_MAX).setLightsEnabled(true).setVibrationEnabled(true)
            .setShowBadge(true).setName(CHANNEL_NAME_LOCAL_MAX).build())
        return channelIdStr
    }

    /** 媒体通知渠道，与 [initNotificationChannel] 普通渠道分离 */
    fun initMediaNotificationChannel(context: Context, isShowN: Boolean = true): String {
        val channelIdStr: String
        if (isShowN) {
            channelIdStr = CHANNEL_ID_MEDIA_MAX
            NotificationManagerCompat.from(context).createNotificationChannel(NotificationChannelCompat.Builder(
                CHANNEL_ID_MEDIA_MAX,
                NotificationManagerCompat.IMPORTANCE_MAX,
            ).setLightsEnabled(true).setVibrationEnabled(true).setShowBadge(true).setName(CHANNEL_NAME_MEDIA_MAX)
                .build())
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
    private var lastBroadEventTime = 0L

    private val broadRecent = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    val reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY).orEmpty()
                    logError("CLOSE_SYSTEM_DIALOGS reason=$reason")
                    if (reason == SYSTEM_DIALOG_REASON_HOME_KEY || reason == SYSTEM_DIALOG_REASON_FS_GESTURE) {
                        tryShowHomeCheckInNotification()
                    }
                }
            }
        }

    }

    /**
     * 注册解锁广播 + Home/手势返回桌面广播
     */
    @Suppress("DEPRECATION")
    fun registerScreenUnlockReceiver(context: Context) {
        if (isRegistered) return
        screenUnlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                logError("registerScreenUnlockReceiver-->${intent?.action}")
                context?.let {
                    when (intent?.action) {
                        Intent.ACTION_USER_PRESENT -> {
                            if (System.currentTimeMillis() - lastBroadEventTime < 10_000) return
                            job?.cancel()
                            job = CoroutineScope(Dispatchers.Main).launch {
                                delay(1000)
                                lastBroadEventTime = System.currentTimeMillis()
                                notificationImplUU.showNotification(it)
                            }
                        }

                        Intent.ACTION_SCREEN_ON -> {
                            if (System.currentTimeMillis() - lastBroadEventTime < 30_000) return
                            job?.cancel()
                            job = CoroutineScope(Dispatchers.Main).launch {
                                delay(4000)
                                lastBroadEventTime = System.currentTimeMillis()
                                notificationImplSU.showNotification(it)
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
            context.registerReceiver(screenUnlockReceiver, filter)
            val filter2 = IntentFilter().apply {
                addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            }
            ContextCompat.registerReceiver(context, broadRecent, filter2, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        isRegistered = true
    }

    /**
     * 本地定时通知（23/59/79分钟轮询）
     */
    fun scheduleLocalNotifications(context: Context) {
        if (isKRAndSam()) return
        DramaWorker.openMe(context)
        registerScreenUnlockReceiver(context)
        if (MMKVRepository.isBuyUser) {
            scheduleInexactAlarm(context)
        } else {
            launcherScope(if (isDebugMode) 3 else 79, 40, {
                showLocalNotification(context, LOCAL_TYPE_79)
            })
        }
        launcherScope(23, if (isDebugMode) 1 else 12, {
            showLocalNotification(context, LOCAL_TYPE_23)
        })
        enqueueLocalWorker(context, WORK_NAME_59, LOCAL_TYPE_59, 59)
        luncherMedia(context)
    }

    private fun luncherMedia(context: Context) {
        val entries = allSceneEntries()
        if (entries.isEmpty()) return
        val (titleRes, descRes) = entries.random()
        CoroutineScope(Dispatchers.Main).launch {
            delay(2500)
            while (isSamsungDevice() && isInApp) {
                delay(1500)
                if (hasNotificationPermission(context).not()) {
                    break
                }
            }
            directImpl(NOTI_ID_MEDIA).showMediaNotification(
                mApp,
                mApp.getString(titleRes),
                mApp.getString(descRes),
            )
        }
    }

    private val workScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private fun launcherScope(time: Int, startDelay: Int, timeCall: () -> Unit) {
        workScope.launch {
            delay(startDelay * 60_000L)
            while (true) {
                timeCall.invoke()
                delay(time * 60000L)
            }
        }
    }

    fun showLocalNotification(context: Context, type: Int, notifId: Int? = null) {
        logError("showLocalNotification--->$type")
        val (titles, descs, index, notificationId) = when (type) {
            LOCAL_TYPE_59 -> Quad(listLocal59Title, listLocal59Content, localIndex59, notifId ?: NOTI_ID_TIMER2)
            LOCAL_TYPE_79 -> Quad(listLocal79Title, listLocal79Content, localIndex79, notifId ?: NOTI_ID_TIMER3)
            else -> Quad(listLocal23Title, listLocal23Content, localIndex23, notifId ?: NOTI_ID_TIMER)
        }
        if (titles.isEmpty() || descs.isEmpty()) return
        val safeIndex = index.coerceAtLeast(0) % titles.size
        val title = context.getString(titles[safeIndex])
        val desc = context.getString(descs[safeIndex])
        when (type) {
            LOCAL_TYPE_59 -> localIndex59 = (safeIndex + 1) % titles.size
            LOCAL_TYPE_79 -> localIndex79 = (safeIndex + 1) % titles.size
            else -> localIndex23 = (safeIndex + 1) % titles.size
        } // 使用专门的通知渠道显示弹窗通知
        directImpl(notificationId).showNotification(context, title, desc)
    }

    private fun enqueueLocalWorker(context: Context, workName: String, type: Int, intervalMin: Long) {
        val request =
            PeriodicWorkRequestBuilder<NotificationWorker>(intervalMin, TimeUnit.MINUTES).setInputData(workDataOf(
                NotificationWorker.KEY_TYPE to type)).setInitialDelay(intervalMin / 3, TimeUnit.MINUTES).build()
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
        if (type == "media") {
            NotificationImpl.lastMediaTime = 0
        }
        localEvent("all_noti_c", hashMapOf("type" to type))
        return type
    }

    private fun getType(id: Int): String {
        return when (id) {
            NOTI_ID_TIMER -> "noti_1"
            NOTI_ID_TIMER2 -> "noti_2"
            NOTI_ID_TIMER3 -> "noti_3"
            NOTI_ID_BACK -> "noti_back" // 后台
            NOTI_ID_HOME_CHECKIN -> "home_checkin"
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

    private var lastSamTime by MMKVData(0L)

    fun showFcmService(context: Context) {
        if (isKRAndSam()) return
        if (isSamsungDevice() && System.currentTimeMillis() - lastSamTime < 60_000 * 30) return
        if (SphereService.isOpenService) return
        lastSamTime = System.currentTimeMillis()
        try {
            ContextCompat.startForegroundService(context, Intent(context, SphereService::class.java))
        } catch (_: Throwable) {
        }
    }


    fun showFcmDataNotification(context: Context, title: String, desc: String) {
        if (hasNotificationPermission(context).not()) {
            logError("no post permission")
            return
        }
        directImpl(NOTI_ID_FCM_DATA).showNotification(context, title, desc)
    }

    fun updateFixedNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).notify(NOTI_ID_FIXED, createFixNotification(context))
        } catch (t: Throwable) {
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
                setOnClickPendingIntent(R.id.layout_root, pendingIntent)
                setOnClickPendingIntent(R.id.tv_withdraw, pendingIntent)
            }

            NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_app)
                .setGroupSummary(false).setSound(null).setGroup("Sphere").setContentTitle(title).setContentText(desc)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(remoteView)
                .setCustomBigContentView(remoteView).setOngoing(true).setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent).build()
        }
        return buildFixedNotification()
    }


    private var time = 0L
    fun onBackShowNotif() {
        if (isInteractive().not()) return
        if (System.currentTimeMillis() - time < Random.nextLong(60_000, 120_000)) return
        time = System.currentTimeMillis()
        val (titleRes, descRes) = allSceneEntries().randomOrNull() ?: return
        directImpl(NOTI_ID_BACK).showNotification(
            mApp,
            mApp.getString(titleRes),
            mApp.getString(descRes),
        )
    }

    /**
     * Home 键 / 全面屏手势回桌面时尝试弹出「签到领奖」通知。
     * 条件：本地时间 9:00–17:59、今日签到尚未领取、当天尚未触发、有通知权限。
     */
    fun tryShowHomeCheckInNotification(): Boolean {
        if (isKRAndSam()) return false
        if (isInteractive().not()) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour !in 10 until 17) return false // 今日已领签到奖励则不弹
        if (TaskHelper.hasClaimableSignInToday().not()) return false
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (lastHomeCheckInDay == day) return false
        if (hasNotificationPermission(mApp).not()) return false
        lastHomeCheckInDay = day
        directImpl(NOTI_ID_HOME_CHECKIN).showNotification(
            mApp,
            mApp.getString(R.string.notification_checkin_title),
            mApp.getString(R.string.notification_checkin_desc),
        )
        return true
    }

    private const val SYSTEM_DIALOG_REASON_KEY = "reason"
    private const val SYSTEM_DIALOG_REASON_HOME_KEY = "homekey"
    private const val SYSTEM_DIALOG_REASON_FS_GESTURE = "fs_gesture"

    private fun allSceneEntries(): List<Pair<Int, Int>> = buildList {
        addAll(pairEntries(listUserUnlockTitle, listUserUnlockContent))
        addAll(pairEntries(listScreenUnlockTitle, listScreenUnlockContent))
        addAll(pairEntries(listLocal23Title, listLocal23Content))
        addAll(pairEntries(listLocal59Title, listLocal59Content))
        addAll(pairEntries(listLocal79Title, listLocal79Content))
    }

    private fun pairEntries(titles: List<Int>, descs: List<Int>): List<Pair<Int, Int>> {
        val size = minOf(titles.size, descs.size)
        return List(size) { index -> titles[index] to descs[index] }
    }

    fun isKRAndSam(): Boolean {
        return "KR" == getFirstsCountry() && isSamsungDevice()
    }

    private var nexCloTime by MMKVData(0L)

    const val ACTION_INEXACT_ALARM = "com.sphere.shortvideos.action.INEXACT_ALARM"
    private const val INEXACT_ALARM_REQUEST_CODE = 988
    private const val INEXACT_ALARM_INTERVAL_MS = 60_000L * 79

    fun scheduleInexactAlarm(context: Context = mApp, force: Boolean = false) {
        if (isKRAndSam()) return
        if (MMKVRepository.isBuyUser.not()) return
        if (!force && nexCloTime > System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + INEXACT_ALARM_INTERVAL_MS
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            INEXACT_ALARM_REQUEST_CODE,
            Intent(context, SphereBroad::class.java).setAction(ACTION_INEXACT_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        AlarmManagerCompat.setAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent,
        )
        nexCloTime = triggerAt
    }

    fun showTime79(context: Context) {
        showLocalNotification(context, LOCAL_TYPE_79)
    }
}