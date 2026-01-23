package com.sphere.shortvideos.helper.mmkv

object MMKVRepository {

    var deviceId by MMKVData("")
    var userFirstCountry by MMKVData("")
    var referrerUrl by MMKVData("")
    var isNeedRequestUMP by MMKVData(true)
    var lastSessionActive by MMKVData(0L)

    var isNewUser by MMKVData(true)

}