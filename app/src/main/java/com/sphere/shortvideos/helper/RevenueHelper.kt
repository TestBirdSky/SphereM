package com.sphere.shortvideos.helper

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.sphere.shortvideos.helper.ad.AdItemBean
import com.sphere.shortvideos.helper.ad.AdPosition
import org.json.JSONObject

object RevenueHelper {

    fun onAdmobRevenueCallback(adValue: AdValue, adBean: AdItemBean, position: AdPosition, responseInfo: ResponseInfo?) {
        val revenue: Double = adValue.valueMicros / 1000000.toDouble()
        firebaseEvent(
            "ad_impression_revenue",
            hashMapOf(
                FirebaseAnalytics.Param.VALUE to revenue,
                FirebaseAnalytics.Param.CURRENCY to adValue.currencyCode
            )
        )
        val loadedSource = responseInfo?.loadedAdapterResponseInfo
        adImpression(JSONObject().apply {
            put("ontario", adValue.valueMicros)
            put("ghoulish", adValue.currencyCode)
            put("auxin", loadedSource?.adSourceName ?: "admob")
            put("friable", adBean.source)
            put("boniface", adBean.adId)
            put("hadamard", position.aliasName)
            put("neva", adBean.format.aliasName)
        })
    }

}