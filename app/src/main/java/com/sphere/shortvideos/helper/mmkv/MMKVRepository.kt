package com.sphere.shortvideos.helper.mmkv

import android.provider.Settings
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.permission.PermissionHelper.showOpenNotifDialogFlag
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import com.sphere.shortvideos.mApp
import java.text.SimpleDateFormat
import java.util.Date

object MMKVRepository {

    var deviceId by MMKVData("")
    var userFirstCountry by MMKVData("")
    var referrerUrl by MMKVData("")
    var installJson by MMKVData("")

    var isNeedRequestUMP by MMKVData(true)
    var lastSessionActive by MMKVData(0L)

    var isNewUser by MMKVData(true)
    var isShowBackTips by MMKVData(true)

    /** [WithdrawReadyDialogFragment] 是否已展示过（全生命周期只弹一次） */
    var hasShownWithdrawReadyDialogEver by MMKVData(false)

    // 用户类型：true 为黑名单用户（Organic），false 为买量用户
    var isBlacklistUser by MMKVData(false)
    /**
     * Adjust attribution.network 解析得到的买量渠道：
     * - mtg: Mintegral
     * - fb: Facebook Ads / Meta
     * - tt: TikTok Ads / ByteDance
     * - unknown: 无法判断或未获取
     */
    var adjustPaidChannel by MMKVData("")

    private var isCurDayStr by MMKVData("") // 当天

    fun checkCueDay(): Boolean {
        //        if (isDebugMode) { //
        //            isCurDayStr = ""
        //        }
        val day = SimpleDateFormat("yyyy-MM-dd").format(Date())
        if (isCurDayStr != day) {
            isCurDayStr = day
            isShowBackTips = true
            showOpenNotifDialogFlag = 30
            AdUtils.allAdShowNum = 0
            WithdrawalActionHelper.showLock2NumCurDay = 0
            WithdrawalActionHelper.curDayAdNum = 0
            return false
        }
        return true
    }

    private var mSphereAndroidId by MMKVData("")

    val androidIdStr by lazy {
        if (mSphereAndroidId.isEmpty()) {
            mSphereAndroidId = Settings.Secure.getString(
                mApp.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: ""
        }
        mSphereAndroidId
    }

}