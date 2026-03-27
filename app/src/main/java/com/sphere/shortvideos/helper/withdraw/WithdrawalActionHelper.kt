package com.sphere.shortvideos.helper.withdraw

import com.chartboost.sdk.impl.fa
import com.google.gson.Gson
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.activity.PangleDramaPlayActivity
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.bean.WithdrawalActionConfig
import com.sphere.shortvideos.dialogs.withdraw.LockInfoDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.PaymentInformationDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.ValuedPlayersDialogFragment
import com.sphere.shortvideos.helper.DialogFragmentDisplayHelper
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.MoneyCacheHelper.watchVideoTime
import com.sphere.shortvideos.helper.RemoteConfHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError

/**
 * 提现动作配置读取（Remote Config key: Withdrawal_action）
 */
object WithdrawalActionHelper {
    private const val REMOTE_KEY = "Withdrawal_action"
    private val gson = Gson()

    @Volatile
    private var cachedConfig: WithdrawalActionConfig? = null
    private var lastRemoteJson = ""

    fun getConfig(): WithdrawalActionConfig {
        cachedConfig?.let { return it }
        val remoteJson = RemoteConfHelper().getString(REMOTE_KEY)
        if (remoteJson.isNotBlank()) {
            lastRemoteJson = remoteJson
        }
        val config = parseConfig(remoteJson) ?: parseConfig(DEFAULT_WITHDRAWAL_ACTION_JSON)
        cachedConfig = config
        return config!!
    }

    fun updateConfigure() {
        val remoteJson = RemoteConfHelper().getString(REMOTE_KEY)
        if (remoteJson.isNotBlank() && remoteJson != lastRemoteJson) {
            lastRemoteJson = remoteJson
            parseConfig(remoteJson)?.let {
                cachedConfig = it
            }
        }
    }

    private fun parseConfig(json: String?): WithdrawalActionConfig? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            gson.fromJson(json, WithdrawalActionConfig::class.java)
        }.getOrNull()
    }

    private var adNum = 0
    var curDayAdNum by MMKVData(0)
    fun addShowAdNum() {
        if (getConfig().isOpenWithdraw().not()) return
        addTask3WatchAdNum()
        if (curTaskProgressStatus > 0) return
        adNum++
        curDayAdNum++
    }

    fun checkIsAllowInDismissAd(activity: GenericActivity) {
        if (DialogFragmentDisplayHelper.hasDialogFragmentShowing(activity)) return
        if (isShowInfoLock()) {
            isShowLock1 =
                DialogFragmentDisplayHelper.show(activity.supportFragmentManager, LockInfoDialogFragment().apply {
                    onSecure = {
                        DialogFragmentDisplayHelper.show(activity.supportFragmentManager,
                            PaymentInformationDialogFragment())
                    }
                }, "lock1", dismissCurrent = false)
            return
        }
        if (activity is PangleDramaPlayActivity) return
        if (isShowInfoLock2()) {
            if (activity is MainActivity) {
                if (DialogFragmentDisplayHelper.show(activity.supportFragmentManager, ValuedPlayersDialogFragment())) {
                    showLock2NumCurDay++
                }
            }
        }
    }

    var isShowLock1 = false
    var showLock2NumCurDay by MMKVData(0)

    private fun isShowInfoLock(): Boolean {
        if (getConfig().isOpenWithdraw().not()) return false
        if (isShowLock1) return false
        if (adNum < getConfig().userMaintenance.withdrawalForm) return false
        if (havaBaseInfo()) return false
        return true
    }

    fun isShowInfoLock2(): Boolean {
        if (getConfig().isOpenWithdraw().not()) return false
        if (WithdrawAmountHelper.isCanWithdraw()) return false
        if (showLock2NumCurDay >= 1) return false
        if (curDayAdNum < getConfig().userMaintenance.premiumUser) return false
        return true
    }

    // 提现账号相关缓存
    var accountWithdrawal by MMKVData("")
    var withdrawalMethodId by MMKVData("")
    var withdrawalValue by MMKVData(0.0) //提的金钱

    // 当前任务状态：TASK1_STEP/TASK2_STEP/TASK3_STEP，0/100 代表结束态
    var curTaskProgressStatus by MMKVData(0)
    var isShowWithApplyDialog by MMKVData(false)

    // ===== 提现任务进度持久化（你后续可在别处写入） =====
    /** task1: Watch xx min drama（存秒，展示时转分钟） */
    private var task1DramaSecond by MMKVData(0)

    /** task2: Claim xx bubbles */
    private var task2BubbleCount by MMKVData(0)

    /** task3: Watch xx ads */
    private var task3AdCount by MMKVData(0)

    fun clearWithdrawalInfoAndAddTask() {
        accountWithdrawal = ""
        curTaskProgressStatus = TASK1_STEP
        isShowWithApplyDialog = false
        resetAllTaskProgress()
    }

    fun taskFinish() {
        isShowWithApplyDialog = true
        curTaskProgressStatus = 0
        withdrawalMethodId = ""
        withdrawalValue = 0.0
        HelperRewardShow.isShowCanCash = true
        MMKVRepository.hasShownWithdrawReadyDialogEver = false
    }

    fun havaBaseInfo(): Boolean {
        return accountWithdrawal.isNotBlank() && withdrawalMethodId.isNotBlank()
    }

    /**
     * @param step 任务步骤：`TASK1_STEP`/`TASK2_STEP`/`TASK3_STEP`
     * @return 当前步骤的“主进度”（task1：分钟；task2：泡泡数；task3：广告次数）
     */
    fun getPrimaryProgressByStep(step: Int): Int {
        return when (step) {
            TASK1_STEP -> task1DramaSecond / 60 // 秒 -> 分钟
            TASK2_STEP -> task2BubbleCount
            TASK3_STEP -> task3AdCount
            else -> 0
        }
    }

    /** 基于当前 [curTaskProgressStatus] 获取主进度 */
    fun getPrimaryProgressByCurStep(): Int = getPrimaryProgressByStep(curTaskProgressStatus)

    /** 仅在任务 1 阶段累加看短剧时长（秒） */
    fun addTask1WatchTime(second: Int) {
        if (curTaskProgressStatus == TASK1_STEP) {
            logError("addTask1WatchTime-->$second --$task1DramaSecond")
            task1DramaSecond += second.coerceAtLeast(0)
        }
    }

    /** 仅在任务 3 阶段累加看广告次数 */
    fun addTask3WatchAdNum() {
        if (curTaskProgressStatus == TASK3_STEP) {
            task3AdCount++
        }
    }

    /** 仅在任务 2 阶段累加泡泡领取次数 */
    fun addTask2BubbleNum() {
        if (curTaskProgressStatus == TASK2_STEP) {
            task2BubbleCount++
        }
    }

    fun resetAllTaskProgress() {
        task1DramaSecond = 0
        task2BubbleCount = 0
        task3AdCount = 0
    }

    fun getCutInProgress(): Int {
        return getConfig().cutIn.random()
    }
}

