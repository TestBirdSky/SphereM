package com.sphere.shortvideos.helper

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.get
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.ad.DramaIntAdHelper
import com.sphere.shortvideos.helper.reward.RewardHelper
import com.sphere.shortvideos.helper.risk.RiskHelper
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mFbAndAdjustHelper
import com.sphere.shortvideos.unlockIndex

class RemoteConfHelper {

    private val remoteConfig by lazy {
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            })
        }
    }

    fun fetch() {
        if (isDebugMode) {
            AdUtils.initData()
            fetchAll()
            remoteConfig.fetchAndActivate().addOnSuccessListener { fetchAll() }
            remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener{
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    logError("onUpdate--->$configUpdate")
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                }
            })
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
        DramaIntAdHelper.updateConfigure()
        WithdrawalActionHelper.updateConfigure()
        fetchFbCon()
    }

    private fun fetchAdRemote() {
        val defaultKey = AD_CONFIG_DEFAULT_KEY
        val key = resolveAdConfigKeyByChannel(MMKVRepository.adjustPaidChannel)
        val channelConfig = runCatching { remoteConfig[key].asString() }.getOrNull().orEmpty()
        if (channelConfig.isNotBlank()) {
            AdUtils.initData(channelConfig)
            return
        }
        // 回退默认配置
        AdUtils.initData(runCatching { remoteConfig[defaultKey].asString() }.getOrNull().orEmpty())
    }

    private fun resolveAdConfigKeyByChannel(channel: String): String {
        return when (channel.lowercase()) {
            "mtg" -> AD_CONFIG_MTG_KEY
            "fb" -> AD_CONFIG_FB_KEY
            "tt" -> AD_CONFIG_TT_KEY
            else -> AD_CONFIG_DEFAULT_KEY
        }
    }

    private fun fetchUnlockIndex() {
        runCatching {
            unlockIndex = remoteConfig["unlock_ep"].asString().toIntOrNull() ?: 4
        }
    }

    private fun fetchRisk() {
        val str = getString("drama_risk_control")
        if (str.isNotEmpty()) {
            RiskHelper.refreshRiskBean(str)
        }
    }

    private fun fetchFbCon() {
        val fbInfo = getString("drama_fb")
        mFbAndAdjustHelper.initFb(fbInfo)
    }

    fun getString(key: String): String = runCatching {
        remoteConfig.getString(key)
    }.getOrNull() ?: ""

    companion object {
        private const val AD_CONFIG_DEFAULT_KEY = "dlmsf_ad_config"
        private const val AD_CONFIG_MTG_KEY = "dlmsf_ad_config_mtg"
        private const val AD_CONFIG_FB_KEY = "dlmsf_ad_config_fb"
        private const val AD_CONFIG_TT_KEY = "dlmsf_ad_config_tt"
    }

}