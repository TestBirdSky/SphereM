package com.sphere.shortvideos.bean

import com.google.gson.annotations.SerializedName

/**
 * Date：2026/1/20
 * Describe: 风控配置Bean
 */
data class RiskBean(
    @SerializedName("ui")
    var ui: UiConfig, // UI 相关风控开关配置

    @SerializedName("behavior")
    var behavior: BehaviorConfig, // 用户行为风控配置

    @SerializedName("device")
    var device: List<String> // 需要检测的设备风险类型列表
)

data class UiConfig(
    @SerializedName("number")
    var number: Int, // 数字联盟检测开关，0 关闭，1 开启

    @SerializedName("behavior")
    var behavior: Int, // 用户异常行为检测开关，0 关闭，1 开启

    @SerializedName("device")
    var device: Int // 设备异常检测开关，0 关闭，1 开启
)

data class BehaviorConfig(
    @SerializedName("ad_short_show")
    var adShortShow: AdShowConfig, // 短时间频繁广告曝光配置

    @SerializedName("ad_short_close")
    var adShortClose: AdCloseConfig, // 短时间频繁关闭广告配置

    @SerializedName("wrong_deem_ad_less")
    var wrongDeemAdLess: Int, // 现金金额达到提现门槛时，广告观看次数下限

    @SerializedName("wrong_deem_ad_more")
    var wrongDeemAdMore: Int, // 观看广告次数上限（未达到提现门槛）

    @SerializedName("no_install")
    var noInstall: Int, // 暂时未使用，TBA 暂时无法归因

    @SerializedName("ad_daily_show")
    var adDailyShow: Int // 每日广告观看次数上限
)

data class AdShowConfig(
    @SerializedName("duration")
    var duration: Int, // 两次广告展示的最小间隔时间（秒）

    @SerializedName("value")
    var value: Int // 超过间隔时间的次数阈值
)

data class AdCloseConfig(
    @SerializedName("duration")
    var duration: Int, // 从广告播放到关闭的最大时间（秒）

    @SerializedName("value")
    var value: Int // 超过时间的关闭次数阈值
)
