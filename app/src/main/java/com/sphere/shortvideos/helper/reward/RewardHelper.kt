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

    private const val REMOTE_KEY = "br_reward"
    private const val REMOTE_ID_KEY = "id_reward"

    private val gson = Gson()

    @Volatile
    private var cachedConfig: RewardConfig? = null

    @Volatile
    private var cachedConfigId: RewardConfig? = null

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

    fun getConfigByLanguage(): RewardConfig {
        return when {
            LauageTools.isIndonesia() -> {
                getIdConfig()
            }

            else -> {
                getBrConfig()
            }
        }
    }


    fun updateConfigure() {
        val remoteJson = RemoteConfHelper().getString(REMOTE_KEY)
        if (remoteJson.isNotBlank() && lastRemoteConfigure != remoteJson) {
            lastRemoteConfigure = remoteJson
            parseConfig(remoteJson)?.let {
                updateBrAndDefaultConfigure(it)
            }
        }
        val remoteIdJson = RemoteConfHelper().getString(REMOTE_ID_KEY)
        if (remoteIdJson.isNotBlank() && lastRemoteConfigureId != remoteIdJson) {
            lastRemoteConfigureId = remoteIdJson
            parseConfig(remoteIdJson)?.let {
                updateIdConfigure(it)
            }
        }
    }

    private fun updateIdConfigure(config: RewardConfig) {
        config.moneyRate = WithdrawAmountHelper.IDR_PER_BRL
        cachedConfigId = config
    }

    private fun updateBrAndDefaultConfigure(config: RewardConfig) {
        config.moneyRate = WithdrawAmountHelper.BRL
        cachedConfig = config
    }

    private fun parseConfig(json: String?): RewardConfig? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            gson.fromJson(json, RewardConfig::class.java)
        }.getOrNull()
    }


}