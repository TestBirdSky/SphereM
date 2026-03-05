package com.sphere.shortvideos.helper

import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.sphere.shortvideos.helper.ad.AdItemBean
import com.sphere.shortvideos.helper.ad.AdPosition
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import com.thinkup.core.api.TUAdInfo
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
            put("hexagon", position.adSense)
            put("neva", adBean.format.aliasName)
        })
        fun postAdmobRevenueAdjust() { //v5
            val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
            adjustAdRevenue.setRevenue(revenue, adValue.currencyCode) //可选配置
            //            adjustAdRevenue.adRevenueUnit = adValue.currencyCode //货币单位
            adjustAdRevenue.adRevenueNetwork = loadedSource?.adSourceName ?: "admob" //广告来源
            adjustAdRevenue.adRevenuePlacement = position.aliasName //广告位
            //发送收益数据
            Adjust.trackAdRevenue(adjustAdRevenue)
            runCatching { //fb purchase
                logFbAdImpression(revenue, adValue.currencyCode)
            }
        }
        postAdmobRevenueAdjust()

    }

    fun onToponRevenueCallback(ad: TUAdInfo, adBean: AdItemBean, position: AdPosition) {
        runCatching { // TopOn 广告价值获取
            val revenue: Double = ad.publisherRevenue
            val currencyCode: String = "USD"
            val adSourceName: String = adBean.source
            firebaseEvent("ad_impression_revenue",
                hashMapOf(FirebaseAnalytics.Param.VALUE to revenue, FirebaseAnalytics.Param.CURRENCY to currencyCode))

            adImpression(JSONObject().apply {
                put("ontario", revenue * 1000000) // 转换为微单位
                put("ghoulish", currencyCode)
                put("auxin", adSourceName)
                put("friable", adBean.source)
                put("boniface", adBean.adId)
                put("hadamard", position.aliasName)
                put("hexagon", position.adSense)
                put("neva", adBean.format.aliasName)
            })

            // Adjust 上报
            val adjustAdRevenue = AdjustAdRevenue("topon_sdk")
            adjustAdRevenue.setRevenue(revenue, currencyCode)
            adjustAdRevenue.adRevenueUnit = ad.adsourceId
            adjustAdRevenue.adRevenueNetwork = adSourceName
            adjustAdRevenue.adRevenuePlacement = position.aliasName
            Adjust.trackAdRevenue(adjustAdRevenue)

            // Facebook 上报
            runCatching {
                logFbAdImpression(revenue, currencyCode)
            }
        }
    }

    fun onMaxRevenueCallback(maxAd: com.applovin.mediation.MaxAd, adBean: AdItemBean, position: AdPosition) {
        runCatching { // MAX 广告价值获取
            val revenue: Double = maxAd.revenue
            val currencyCode: String = "USD"
            val networkName: String = maxAd.networkName ?: "max"
            firebaseEvent("ad_impression_revenue",
                hashMapOf(FirebaseAnalytics.Param.VALUE to revenue, FirebaseAnalytics.Param.CURRENCY to currencyCode))
            adImpression(JSONObject().apply {
                put("ontario", revenue * 1000000) // 转换为微单位
                put("ghoulish", currencyCode)
                put("auxin", networkName)
                put("friable", adBean.source)
                put("boniface", adBean.adId)
                put("hadamard", position.aliasName)
                put("hexagon", position.adSense)
                put("neva", adBean.format.aliasName)
            })
            // Adjust 上报
            val adjustAdRevenue = AdjustAdRevenue("applovin_max_sdk")
            adjustAdRevenue.setRevenue(revenue, currencyCode)
            adjustAdRevenue.adRevenueUnit = maxAd.adUnitId
            adjustAdRevenue.adRevenueNetwork = networkName
            adjustAdRevenue.adRevenuePlacement = position.aliasName
            Adjust.trackAdRevenue(adjustAdRevenue)
            logError("FB-->")
            // Facebook 上报
            runCatching {
                logFbAdImpression(revenue, currencyCode)
            }
        }
    }

    /** 向 Facebook SDK 上报 AD_IMPRESSION（Meta 广告收入标准事件），与 logPurchase 并存 */
    private fun logFbAdImpression(revenue: Double, currencyCode: String) {
        runCatching {
            val logger = AppEventsLogger.newLogger(mApp)
            val params = Bundle().apply {
                putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currencyCode)
            }
            logger.logEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, revenue, params)
            logger.logPurchase((revenue).toBigDecimal(), Currency.getInstance(currencyCode))
        }
    }
}