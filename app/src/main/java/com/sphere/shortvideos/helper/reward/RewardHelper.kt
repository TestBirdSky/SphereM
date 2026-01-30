package com.sphere.shortvideos.helper.reward

import com.google.gson.Gson
import com.sphere.shortvideos.bean.RewardConfig
import com.sphere.shortvideos.helper.LauageTools
import com.sphere.shortvideos.helper.RemoteConfHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper

/**
 * Date：2026/1/22
 * Describe: BR 奖励配置读取
 */
object RewardHelper {
    private var lastRemoteConfigure = ""
    private var lastRemoteConfigureId = ""
    private var lastRemoteConfigureUsd = ""

    private const val REMOTE_KEY = "br_reward"
    private const val REMOTE_ID_KEY = "id_reward"
    private const val REMOTE_US_KEY = "us_reward"

    private val gson = Gson()

    @Volatile
    private var cachedConfig: RewardConfig? = null

    @Volatile
    private var cachedConfigId: RewardConfig? = null

    @Volatile
    private var cachedConfigUsd: RewardConfig? = null

    private fun getBrConfig(): RewardConfig {
        cachedConfig?.let { return it }
        val remoteJson = RemoteConfHelper().getString(REMOTE_KEY)
        if (remoteJson.isNotBlank()) {
            lastRemoteConfigure = remoteJson
        }
        val config = parseConfig(remoteJson) ?: parseConfig(DEFAULT_JSON)
        updateBrAndDefaultConfigure(config!!)
        return config
    }

    private fun getIdConfig(): RewardConfig {
        cachedConfigId?.let { return it }
        val remoteJson = RemoteConfHelper().getString(REMOTE_ID_KEY)
        if (remoteJson.isNotBlank()) {
            lastRemoteConfigureId = remoteJson
        }
        val config = parseConfig(remoteJson) ?: parseConfig(DEFAULT_ID_JSON)
        updateIdConfigure(config!!)
        return config
    }

    private fun getEnConfig(): RewardConfig {
        cachedConfigUsd?.let { return it }
        val remoteJson = RemoteConfHelper().getString(REMOTE_US_KEY)
        if (remoteJson.isNotBlank()) {
            lastRemoteConfigureUsd = remoteJson
        }
        val config = parseConfig(remoteJson) ?: parseConfig(DEFAULT_US_JSON)
        updateEnConfigure(config!!)
        return config
    }

    fun getConfigByLanguage(): RewardConfig {
        return when {
            LauageTools.isIndonesia() -> getIdConfig()
            LauageTools.isBrazil() -> getBrConfig()
            else -> getEnConfig()
        }
    }


    fun updateConfigure() {
        val remoteJson = RemoteConfHelper().getString(REMOTE_KEY)
        if (remoteJson.isNotBlank() && lastRemoteConfigure != remoteJson) {
            lastRemoteConfigure = remoteJson
            parseConfig(remoteJson)?.let { updateBrAndDefaultConfigure(it) }
        }
        val remoteIdJson = RemoteConfHelper().getString(REMOTE_ID_KEY)
        if (remoteIdJson.isNotBlank() && lastRemoteConfigureId != remoteIdJson) {
            lastRemoteConfigureId = remoteIdJson
            parseConfig(remoteIdJson)?.let { updateIdConfigure(it) }
        }
        val remoteUsJson = RemoteConfHelper().getString(REMOTE_US_KEY)
        if (remoteUsJson.isNotBlank() && lastRemoteConfigureUsd != remoteUsJson) {
            lastRemoteConfigureUsd = remoteUsJson
            parseConfig(remoteUsJson)?.let { updateEnConfigure(it) }
        }
    }

    private fun updateIdConfigure(config: RewardConfig) {
        cachedConfigId = config
    }

    private fun updateEnConfigure(config: RewardConfig) {
        cachedConfigUsd = config
    }

    private fun updateBrAndDefaultConfigure(config: RewardConfig) {
        cachedConfig = config
    }

    private fun parseConfig(json: String?): RewardConfig? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            gson.fromJson(json, RewardConfig::class.java)
        }.getOrNull()
    }


}