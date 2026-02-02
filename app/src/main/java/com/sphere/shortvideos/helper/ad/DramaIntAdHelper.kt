package com.sphere.shortvideos.helper.ad

import com.google.gson.Gson
import com.sphere.shortvideos.bean.DramaIntAdConfig
import com.sphere.shortvideos.bean.DramaIntAdRange
import com.sphere.shortvideos.helper.AppHelper
import com.sphere.shortvideos.helper.LauageTools
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.RemoteConfHelper
import com.sphere.shortvideos.logError
import kotlin.random.Random

object DramaIntAdHelper {
    private const val REMOTE_KEY = "drama_int_ad"
    private const val ENCRYPT_CODE = 42 // 加密密钥
    private val gson = Gson()

    @Volatile
    private var cachedConfig: DramaIntAdConfig? = null
    private var lastRemoteJson = ""

    fun getConfig(): DramaIntAdConfig {
        cachedConfig?.let { return it }
        val remoteJson = RemoteConfHelper().getString(REMOTE_KEY)
        if (remoteJson.isNotBlank()) {
            lastRemoteJson = remoteJson
        }
        // 解密默认配置
        val config = parseConfig(remoteJson) ?: parseConfig(AppHelper.decrypt(DEFAULT_DRAMA_INT_AD_JSON, ENCRYPT_CODE))
        cachedConfig = config
        return config!!
    }

    fun getRangesByLanguage(): List<DramaIntAdRange> {
        val config = getConfig()
        return when {
            LauageTools.isIndonesia() -> config.idIntAd
            LauageTools.isBrazil() -> config.brIntAd
            else -> config.usIntAd
        }
    }

    fun fetchIsShowRateAd(): Boolean {
        val conL = getRangesByLanguage()
        val moneyCur = MoneyCacheHelper.fetchCurMoney()
        val rate = conL.firstOrNull {
            it.isInRange(moneyCur)
        }?.point ?: 0
        val randomResult = Random.nextInt(1, 100)
        return rate >= randomResult
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

    private fun parseConfig(json: String?): DramaIntAdConfig? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            gson.fromJson(json, DramaIntAdConfig::class.java)
        }.getOrNull()
    }
}
