package com.sphere.shortvideos.helper.risk

import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.helper.AppHelper
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
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
 * Date：2026/1/29
 * Describe:
 */
class IpCheckHelper {
    private val baseUrl = GlobalConstants.RISK_URL
    private val scop = CoroutineScope(Dispatchers.IO)
    private val okHttpClient = OkHttpClient()

    fun netRequest() {
        val jsonString = JSONObject().put("ashark", MMKVRepository.androidIdStr).toString()
        val request =
            Request.Builder().post(jsonString.toRequestBody("application/json".toMediaTypeOrNull())).url(baseUrl)
                .build()
        post(request, 20)
    }

    private fun post(request: Request, retry: Int = 3) {
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val js = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    runCatching {
                        val re = AppHelper.decrypt(js, 38)
                        logError(re)
                        val b = JSONObject(re).optJSONObject("data")?.optBoolean("bsnake", false) ?: ""
                        HelperRiskNetCheck.checkIpStatus = b.toString()
                    }

                } else {
                    scop.launch {
                        delay(150000)
                        if (retry > 0) post(request, retry - 1)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                logError("netRequest-->$e")
                scop.launch {
                    delay(80000)
                    if (retry > 0) post(request, retry - 1)
                }
            }
        })
    }

}