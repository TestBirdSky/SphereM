package com.sphere.shortvideos.helper

import android.content.Context
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import java.util.Locale

/**
 * Date：2026/1/20
 * Describe: 语言工具类，支持英语（默认）、巴西葡语、印尼语
 */
object LauageTools {

    /**
     * 国家代码常量
     */
    object CountryCode {
        const val BRAZIL = "BR"
        const val INDONESIA = "ID"
        const val US = "US"
    }

    /**
     * 语言代码常量
     */
    object LanguageCode {
        const val ENGLISH = "en"
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
     * 获取当前设备对应的国家代码
     * 优先根据语言判断，语言确定了就返回对应国家
     * 如果语言不匹配，再根据国家代码判断
     * @return 国家代码（BR/ID/US），默认返回 US
     */
    fun getCurrentCountry(): String {
        val locale = getDeviceLanguage(mApp)
        val language = locale.language.lowercase()
        val country = locale.country
        val languageTag = locale.toLanguageTag().lowercase()
        
        // 优先语言判断：语言确定了国家就确定了
        val countryByLanguage = when (language) {
            LanguageCode.PORTUGUESE -> CountryCode.BRAZIL
            LanguageCode.INDONESIAN, "in" -> CountryCode.INDONESIA  // 兼容非标准的 in 语言代码
            LanguageCode.ENGLISH -> CountryCode.US
            else -> null
        }
        
        // 如果通过语言确定了国家，直接返回
        if (countryByLanguage != null) {
            return countryByLanguage
        }
        
        // 兼容语言标签格式（如 in-ID, id-ID）
        when {
            languageTag.startsWith("in-") || languageTag.startsWith("id-") -> return CountryCode.INDONESIA
            languageTag.startsWith("pt-") -> return CountryCode.BRAZIL
        }
        
        // 语言都没有匹配，再根据国家代码判断
        return when (country) {
            CountryCode.BRAZIL -> CountryCode.BRAZIL
            CountryCode.INDONESIA -> CountryCode.INDONESIA
            else -> CountryCode.US  // 默认返回美国
        }
    }

    /**
     * 判断是否为巴西用户
     * @return true 是巴西用户，false 不是
     */
    fun isBrazil(): Boolean {
        return getCurrentCountry() == CountryCode.BRAZIL
    }

    /**
     * 判断是否为印尼用户
     * @return true 是印尼用户，false 不是
     */
    fun isIndonesia(): Boolean {
        return getCurrentCountry() == CountryCode.INDONESIA
    }

    /**
     * 判断是否为英语用户（默认，非巴西/印尼即视为英语）
     * @return true 是英语用户，false 不是
     */
    fun isEnglish(): Boolean {
        return !isBrazil() && !isIndonesia()
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
     * 兼容非标准的 "in" 语言代码
     * @return true 是印尼语用户，false 不是
     */
    fun isIndonesianUser(context: Context): Boolean {
        val languageCode = getDeviceLanguageCode(context).lowercase()
        return languageCode == LanguageCode.INDONESIAN || languageCode == "in"
    }


    /**
     * 根据国家/语言获取对应的 Locale
     * @param countryCode 国家代码（BR/ID/US）或语言代码（en/pt/id）
     * @return Locale 对象，未匹配时返回英语（默认）
     */
    fun getLocaleByCountry(countryCode: String): Locale {
        return when (countryCode) {
            CountryCode.BRAZIL -> Locale(LanguageCode.PORTUGUESE, CountryCode.BRAZIL)
            CountryCode.INDONESIA -> Locale(LanguageCode.INDONESIAN, CountryCode.INDONESIA)
            CountryCode.US, LanguageCode.ENGLISH -> Locale.ENGLISH
            else -> Locale.ENGLISH
        }
    }

    /**
     * 获取当前业务语言对应的 Locale（英语 / 巴西葡语 / 印尼语）
     */
    fun getAppLocale(): Locale {
        return when {
            isIndonesia() -> getLocaleByCountry(CountryCode.INDONESIA)
            isBrazil() -> getLocaleByCountry(CountryCode.BRAZIL)
            else -> Locale.ENGLISH
        }
    }
}