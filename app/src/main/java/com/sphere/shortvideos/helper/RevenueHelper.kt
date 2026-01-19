package com.sphere.shortvideos.helper

import com.google.android.libraries.ads.mobile.sdk.common.AdSourceResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.firebase.analytics.FirebaseAnalytics
import com.sphere.shortvideos.helper.ad.AdItemBean
import com.sphere.shortvideos.helper.ad.AdPosition
import org.json.JSONObject

object RevenueHelper {

    fun onAdmobRevenueCallback(adValue: AdValue, adBean: AdItemBean, position: AdPosition, loadedAdSource: AdSourceResponseInfo?) {
        val revenue: Double = adValue.valueMicros / 1000000.toDouble()
        firebaseEvent(
            "ad_impression_revenue",
            hashMapOf(
                FirebaseAnalytics.Param.VALUE to revenue,
                FirebaseAnalytics.Param.CURRENCY to adValue.currencyCode
            )
        )
        adImpression(JSONObject().apply {
            put("ontario", adValue.valueMicros)
            put("ghoulish", adValue.currencyCode)
            put("auxin", loadedAdSource?.name ?: "admob")
            put("friable", adBean.source)
            put("boniface", adBean.adId)
            put("hadamard", position.aliasName)
            put("neva", adBean.format.aliasName)
        })
    }

}