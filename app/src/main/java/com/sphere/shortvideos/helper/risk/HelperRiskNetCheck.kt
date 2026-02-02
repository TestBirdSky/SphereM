package com.sphere.shortvideos.helper.risk

import android.content.Context
import cn.shuzilm.core.Main
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.logError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/**
 * Date：2026/1/20
 * Describe:
 */
object HelperRiskNetCheck {
    var checkIpStatus by MMKVData("")
    private val mIpCheckHelper by lazy { IpCheckHelper() }
    var riskDevType by MMKVData(0)

    private val scop = CoroutineScope(Dispatchers.IO)
    private val okHttpClient = OkHttpClient()

    // 新加坡
    private val baseUrl = "https://sg-ddi.shuzilm.cn/q"

    private fun init(context: Context) {
        Main.init(
            context,
            "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMYl4KczbxQYcRCOgSH0lzRtfuI/jffXOXpHUXRVm3CRiyNL4M5U0Vy3qC+HO64/a1ZZ2FFcKLG69oOvUkCuMr0CAwEAAQ==",
        )
    }


    fun requestHerUser(context: Context) {
        if (checkIpStatus.isEmpty()) {
            mIpCheckHelper.netRequest()
        } // 必须先初始化，再调用其他方法
        logError("requestHerUser-->$checkIpStatus")
        init(context)
        Main.getQueryID(context, "", "", true) { p0 ->
            logError("requestHerUser--->$p0")
            if (p0.isNullOrEmpty()) return@getQueryID
            val jsonString =
                JSONObject().put("protocol", 2).put("did", p0).put("pkg", context.packageName).toString()
            reqRisk(jsonString, 0)
        }
    }

    private fun reqRisk(jsonString: String, retry: Int) {
        val request =
            Request.Builder().post(jsonString.toRequestBody("application/json".toMediaTypeOrNull())).url(baseUrl)
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val js = response.body?.string() ?: ""
                    logError(js)
                    runCatching {
                        riskDevType = JSONObject(js).optInt("device_type", 0)
                    }
                } else {
                    scop.launch {
                        delay(310000)
                        if (retry <= 1) reqRisk(jsonString, retry + 1)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                scop.launch {
                    delay(110000)
                    if (retry <= 3) reqRisk(jsonString, retry + 1)
                }
            }
        })
    }

}