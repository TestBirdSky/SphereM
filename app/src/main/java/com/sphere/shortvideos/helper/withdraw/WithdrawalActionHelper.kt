package com.sphere.shortvideos.helper.withdraw

import com.google.gson.Gson
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.activity.PangleDramaPlayActivity
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.bean.WithdrawalActionConfig
import com.sphere.shortvideos.dialogs.LockInfoDialogFragment
import com.sphere.shortvideos.dialogs.PaymentInformationDialogFragment
import com.sphere.shortvideos.dialogs.ValuedPlayersDialogFragment
import com.sphere.shortvideos.helper.DialogFragmentDisplayHelper
import com.sphere.shortvideos.helper.RemoteConfHelper
import com.sphere.shortvideos.helper.mmkv.MMKVData

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
        adNum++
        curDayAdNum++
    }

    fun checkIsAllowInDismissAd(activity: GenericActivity) {
        if (isShowInfoLock()) {
            isShowLock1 = DialogFragmentDisplayHelper
                .show(activity.supportFragmentManager, LockInfoDialogFragment().apply {
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
                DialogFragmentDisplayHelper.show(activity.supportFragmentManager, ValuedPlayersDialogFragment())
                showLock2NumCurDay++
            }
        }
    }

    var isShowLock1 = false
    var showLock2NumCurDay by MMKVData(0)

    private fun isShowInfoLock(): Boolean {
        if (getConfig().isOpenWithdraw().not()) return false
        if (isShowLock1) return false
        if (adNum < getConfig().userMaintenance.withdrawalForm) return false
        return true
    }

    fun isShowInfoLock2(): Boolean {
        if (getConfig().isOpenWithdraw().not()) return false
        if (showLock2NumCurDay >= 1) return false
        if (curDayAdNum < getConfig().userMaintenance.premiumUser) return false
        return true
    }
}

