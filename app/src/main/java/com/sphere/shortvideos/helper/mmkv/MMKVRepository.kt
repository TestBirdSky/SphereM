package com.sphere.shortvideos.helper.mmkv

import com.sphere.shortvideos.helper.permission.PermissionHelper.showOpenNotifDialogFlag
import com.sphere.shortvideos.isDebugMode
import java.text.SimpleDateFormat
import java.util.Date

object MMKVRepository {

    var deviceId by MMKVData("")
    var userFirstCountry by MMKVData("")
    var referrerUrl by MMKVData("")
    var isNeedRequestUMP by MMKVData(true)
    var lastSessionActive by MMKVData(0L)

    var isNewUser by MMKVData(true)
    var isShowBackTips by MMKVData(true)

    private var isCurDayStr by MMKVData("") // 当天

    fun checkCueDay(): Boolean {
        if (isDebugMode) {
            isCurDayStr = ""
        }
        val day = SimpleDateFormat("yyyy-MM-dd").format(Date())
        if (isCurDayStr != day) {
            isCurDayStr = day
            isShowBackTips = true
            showOpenNotifDialogFlag = 30
            return false
        }
        return true
    }

}