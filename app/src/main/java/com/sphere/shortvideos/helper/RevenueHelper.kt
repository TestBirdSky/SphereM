package com.sphere.shortvideos.helper

import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.sphere.shortvideos.helper.ad.AdItemBean
import com.sphere.shortvideos.helper.ad.AdPosition
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import org.json.JSONObject
import java.util.Currency

object RevenueHelper {

    fun onAdmobRevenueCallback(adValue: AdValue,
                               adBean: AdItemBean,
                               position: AdPosition,
                               responseInfo: ResponseInfo?) {
        val revenue: Double = adValue.valueMicros / 1000000.toDouble()
        firebaseEvent("ad_impression_revenue",
            hashMapOf(FirebaseAnalytics.Param.VALUE to revenue,
                FirebaseAnalytics.Param.CURRENCY to adValue.currencyCode))
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
        fun postAdmobRevenueAdjust() { //v5
            val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
            adjustAdRevenue.setRevenue(revenue, adValue.currencyCode)
            //可选配置
            adjustAdRevenue.adRevenueUnit = adValue.currencyCode //货币单位
            adjustAdRevenue.adRevenueNetwork = loadedSource?.adSourceName ?: "admob" //广告来源
            adjustAdRevenue.adRevenuePlacement = position.aliasName //广告位
            //发送收益数据
            Adjust.trackAdRevenue(adjustAdRevenue)
            runCatching { //fb purchase
                AppEventsLogger.newLogger(mApp).logPurchase((revenue).toBigDecimal(), Currency.getInstance(adValue.currencyCode))
            }
        }
        postAdmobRevenueAdjust()

    }

}