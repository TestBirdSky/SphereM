package com.sphere.shortvideos.helper

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.get
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.gson.Gson
import com.sphere.shortvideos.bean.RiskBean
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.mFbAndAdjustHelper
import com.sphere.shortvideos.riskBean
import com.sphere.shortvideos.unlockIndex

class RemoteConfHelper {

    private val remoteConfig by lazy {
        Firebase.remoteConfig.apply { setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 }) }
    }

    fun fetch() {
        if (isDebugMode) {
            AdUtils.initData()
        } else {
            fetchAll()
            remoteConfig.fetchAndActivate().addOnSuccessListener { fetchAll() }
        }
    }

    private fun fetchAll() {
        fetchAdRemote()
        fetchUnlockIndex()
        fetchRisk()
        RewardHelper.updateConfigure()
    }

    private fun fetchAdRemote() {
        AdUtils.initData(remoteConfig["ds_ad_config"].asString())
    }

    private fun fetchUnlockIndex() {
        runCatching {
            unlockIndex = remoteConfig["unlock_ep"].asString().toIntOrNull() ?: 4
        }
    }

    private fun fetchRisk(){
        val str = remoteConfig.getString("risk_control")
        runCatching {
            val bean = Gson().fromJson(str, RiskBean::class.java)
            riskBean = bean
        }
        val fbInfo = remoteConfig.getString("drama_fb")
        mFbAndAdjustHelper.initFb(fbInfo)
    }

    fun getString(key: String): String = remoteConfig.getString(key)


}