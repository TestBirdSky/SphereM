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
        const val ARABIC = "ar"
    }

    private val SUPPORTED_LANGUAGE_CODES = setOf(
        LanguageCode.ENGLISH,
        "ar",
        "th",
        "es",
    )

    private val LANGUAGE_WHITELIST = setOf(
        "zh_hans",
        "zh_hant",
        "en",
        "vi",
        "in",
        "th",
        "ja",
        "ko",
        "pt",
        "es",
    )

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
     * 若系统语言在支持列表内：按语言路由（pt->BR, id/in->ID, 其余支持语言->US）
     * 若系统语言不在支持列表：按国家码兜底
     * @return 国家代码（BR/ID/US），默认返回 US
     */
    fun getCurrentCountry(): String {
        val locale = getDeviceLanguage(mApp)
        val language = locale.language.lowercase()
        val country = locale.country.uppercase()
        val languageTag = locale.toLanguageTag().lowercase()

        // 巴西/印尼优先直返，不参与英语兜底
        when (language) {
            LanguageCode.PORTUGUESE -> return CountryCode.BRAZIL
            LanguageCode.INDONESIAN, "in" -> return CountryCode.INDONESIA
        }

        if (isSupportedLanguage(language, languageTag)) {
            return CountryCode.US
        }

        // 不在支持语言列表时，仍按国家码兜底
        return when (country) {
            CountryCode.BRAZIL -> CountryCode.BRAZIL
            CountryCode.INDONESIA -> CountryCode.INDONESIA
            else -> CountryCode.US
        }
    }

    private fun isSupportedLanguage(language: String, languageTag: String): Boolean {
        if (SUPPORTED_LANGUAGE_CODES.contains(language)) return true
        return SUPPORTED_LANGUAGE_CODES.any { code -> languageTag.startsWith("$code-") }
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
     * 判断是否为阿拉伯语用户
     * @return true 是阿拉伯语用户，false 不是
     */
    fun isArabicUser(context: Context): Boolean {
        return getDeviceLanguageCode(context).lowercase() == LanguageCode.ARABIC
    }

    /**
     * 判断当前设备是否为阿拉伯语
     * @return true 是阿拉伯语，false 不是
     */
    fun isArabic(): Boolean {
        return isArabicUser(mApp)
    }

    /**
     * 判断当前设备语言是否在白名单中
     * 白名单：zh_hans, zh_hant, en, vi, in(id兼容), th, ja, ko, pt, es
     */
    fun isCurrentLanguageInWhitelist(): Boolean {
        return isLanguageInWhitelist(mApp)
    }

    /**
     * 判断指定 context 的设备语言是否在白名单中
     * 白名单：zh_hans, zh_hant, en, vi, in(id兼容), th, ja, ko, pt, es
     */
    fun isLanguageInWhitelist(context: Context): Boolean {
        val normalizedLanguage = normalizeLanguageKey(getDeviceLanguage(context))
        return LANGUAGE_WHITELIST.contains(normalizedLanguage)
    }

    private fun normalizeLanguageKey(locale: Locale): String {
        val language = locale.language.lowercase()
        val country = locale.country.uppercase()
        val languageTag = locale.toLanguageTag().lowercase()
//
//        if (language == "zh") {
//            // 简繁体优先使用 script 判断；无 script 时按地区兜底
//            return when {
//                languageTag.contains("-hant") -> "zh_hant"
//                languageTag.contains("-hans") -> "zh_hans"
//                country in setOf("TW", "HK", "MO") -> "zh_hant"
//                else -> "zh_hans"
//            }
//        }

        // 印尼语统一返回 in（兼容 Android 里可能出现的 id）
        if (language == "id" || language == "in") {
            return "in"
        }

        return language
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