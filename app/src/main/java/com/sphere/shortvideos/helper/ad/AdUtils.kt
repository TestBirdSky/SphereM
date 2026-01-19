package com.sphere.shortvideos.helper.ad

import com.sphere.shortvideos.GlobalConstants
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject

object AdUtils {

    val adScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, _ -> }) }
    val launchHolder = AdHolder(LaunchPosition)
    val unlockHolder = AdHolder(UnlockPosition)

    fun initData(json: String = GlobalConstants.DEFAULT_JSON) {
        val adJson = json.ifBlank { GlobalConstants.DEFAULT_JSON }
        runCatching {
            JSONObject(adJson).apply {
                launchHolder.initHolder(LaunchPosition.aliasName.formatBean(this))
                unlockHolder.initHolder(UnlockPosition.aliasName.formatBean(this))
            }
        }
    }

    private fun String.formatBean(obj: JSONObject): List<AdItemBean> {
        val result = mutableListOf<AdItemBean>()
        val jsonArray = obj.optJSONArray(this) ?: return result
        runCatching {
            for (i in 0 until jsonArray.length()) {
                val itemObj = jsonArray.getJSONObject(i)
                val format = when (itemObj.optString("dsty")) {
                    "op" -> AppOpenFormat
                    else -> InterstitialFormat
                }
                result.add(
                    AdItemBean(
                        adId = itemObj.optString("dsid"),
                        source = itemObj.optString("amtt"),
                        format = format,
                        timeout = itemObj.optInt("dsad", 0),
                        weight = itemObj.optInt("dsei", 0),
                    )
                )
            }
        }
        return result
    }

}