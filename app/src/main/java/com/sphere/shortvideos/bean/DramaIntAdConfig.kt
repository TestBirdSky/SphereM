package com.sphere.shortvideos.bean

import com.google.gson.annotations.SerializedName

data class DramaIntAdConfig(
    @SerializedName("br_int_ad") val brIntAd: List<DramaIntAdRange>,
    @SerializedName("id_int_ad") val idIntAd: List<DramaIntAdRange>,
    @SerializedName("us_int_ad") val usIntAd: List<DramaIntAdRange>,
)

data class DramaIntAdRange(
    @SerializedName("first_number") val firstNumber: Int,
    @SerializedName("point") val point: Int,
    @SerializedName("end_number") val endNumber: Int?,
) {
    fun isInRange(value: Double): Boolean {
        if (value < firstNumber) return false
        return value < (endNumber ?: Int.MAX_VALUE)
    }
}
