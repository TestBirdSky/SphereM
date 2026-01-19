package com.sphere.shortvideos.helper

import android.os.Build
import com.sphere.shortvideos.BuildConfig
import com.sphere.shortvideos.helper.mmkv.MMKVRepository.deviceId
import com.sphere.shortvideos.helper.mmkv.MMKVRepository.userFirstCountry
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import java.util.Locale
import java.util.UUID

object EventData {

    private val baseUrl = if (isDebugMode) "https://test-sperry.dramasphere.link/integral/rotten/wigmake" else "https://sperry.dramasphere.link/waterway/croatia/tenney"
    private val okHttpClient by lazy { OkHttpClient.Builder().build() }
    val distinctId by lazy { fetchDeviceId() }
    val firstCountry by lazy { fetchCountryCode() }
    private val eventScope by lazy { CoroutineScope(Dispatchers.IO + SupervisorJob()) }

    fun buildBody(): JSONObject {
        return JSONObject().apply {
            put("swap", JSONObject().apply {
                put("ge", "brocade")
                put("jorge", "")
                put("sumac", Build.MANUFACTURER ?: "")
            })
            put("aspheric", JSONObject().apply {
                put("sizzle", Build.BRAND ?: "")
                put("oxbow", "")
            })
            put("inhuman", JSONObject().apply {
                put("afford", mApp.packageName)
                put("few", distinctId)
                put("eccles", BuildConfig.VERSION_NAME)
                put("baja", UUID.randomUUID().toString())
                put("feldspar", firstCountry)
                put("lineage", Build.MODEL ?: "")
                put("amharic", Build.VERSION.RELEASE ?: "")
                put("jill", Locale.getDefault().toString())
            })
            put("trypsin", JSONObject().apply {
                put("sulphur", System.currentTimeMillis())
                put("alkaline", "")
            })
        }
    }

    fun eventCall(obj: JSONObject) {
        val jsonString = obj.toString()
        var retry = 0

        fun callEvent() {
            eventScope.launch {
                val request = Request.Builder().post(jsonString.toRequestBody("application/json".toMediaTypeOrNull())).url(baseUrl).build()
                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        logError(response.body?.string())
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        launch {
                            delay(100000L)
                            if (++retry <= 3) callEvent()
                        }
                    }
                })
            }
        }
        callEvent()
    }

    private fun fetchCountryCode(): String {
        return userFirstCountry.ifBlank {
            val country = Locale.getDefault().country
            userFirstCountry = country
            return@ifBlank country
        }
    }

    private fun fetchDeviceId(): String {
        return deviceId.ifBlank {
            val id = UUID.randomUUID().toString()
            deviceId = id
            return@ifBlank id
        }
    }

}