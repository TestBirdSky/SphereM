package com.sphere.shortvideos.helper

import android.content.Context
import com.sphere.shortvideos.mApp
import java.util.Locale

/**
 * Date：2026/1/20
 * Describe: 语言工具类，主要用于判断巴西和印尼国家
 */
object LauageTools {

    /**
     * 国家代码常量
     */
    object CountryCode {
        const val BRAZIL = "BR"
        const val INDONESIA = "ID"
    }

    /**
     * 语言代码常量
     */
    object LanguageCode {
        const val PORTUGUESE = "pt"
        const val INDONESIAN = "id"
    }

    /**
     * 获取当前设备语言
     */
    fun getDeviceLanguage(context: Context): Locale {
        return context.resources.configuration.locales[0]
    }

    /**
     * 获取当前设备语言代码
     */
    fun getDeviceLanguageCode(context: Context): String {
        return getDeviceLanguage(context).language
    }

    /**
     * 获取当前设备国家代码
     */
    fun getDeviceCountryCode(context: Context): String {
        return getDeviceLanguage(context).country
    }

    /**
     * 获取完整的语言标签（如 pt-BR, id-ID）
     */
    fun getDeviceLanguageTag(context: Context): String {
        return getDeviceLanguage(context).toLanguageTag()
    }

    /**
     * 判断是否为巴西用户
     * @return true 是巴西用户，false 不是
     */
    fun isBrazil(): Boolean {
        val locale = getDeviceLanguage(mApp)
        return locale.country == CountryCode.BRAZIL ||
                locale.language == LanguageCode.PORTUGUESE
    }

    /**
     * 判断是否为印尼用户
     * @return true 是印尼用户，false 不是
     */
    fun isIndonesia(): Boolean {
        val locale = getDeviceLanguage(mApp)
        return locale.country == CountryCode.INDONESIA ||
                locale.language == LanguageCode.INDONESIAN
    }


    /**
     * 判断是否为葡萄牙语用户（包括巴西和其他葡萄牙语国家）
     * @return true 是葡萄牙语用户，false 不是
     */
    fun isPortugueseUser(context: Context): Boolean {
        return getDeviceLanguageCode(context) == LanguageCode.PORTUGUESE
    }

    /**
     * 判断是否为印尼语用户
     * @return true 是印尼语用户，false 不是
     */
    fun isIndonesianUser(context: Context): Boolean {
        return getDeviceLanguageCode(context) == LanguageCode.INDONESIAN
    }


    /**
     * 根据国家获取对应的语言环境
     * @param countryCode 国家代码
     * @return Locale 对象
     */
    fun getLocaleByCountry(countryCode: String): Locale {
        return when (countryCode) {
            CountryCode.BRAZIL -> Locale(LanguageCode.PORTUGUESE, CountryCode.BRAZIL)
            CountryCode.INDONESIA -> Locale(LanguageCode.INDONESIAN, CountryCode.INDONESIA)
            else -> Locale.getDefault()
        }
    }


}