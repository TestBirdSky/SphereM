package com.sphere.shortvideos.helper

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.get
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.unlockIndex

class RemoteConfHelper {

    private val remoteConfig by lazy {
        Firebase.remoteConfig.apply { setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 }) }
    }

    fun fetch() {
        if (isDebugMode) {
            AdUtils.initData()
        } else {
            fetchAll()
            remoteConfig.fetchAndActivate().addOnSuccessListener { fetchAll() }
        }
    }

    private fun fetchAll() {
        fetchAdRemote()
        fetchUnlockIndex()
    }

    private fun fetchAdRemote() {
        AdUtils.initData(remoteConfig["ds_ad_config"].asString())
    }

    private fun fetchUnlockIndex() {
        runCatching {
            unlockIndex = remoteConfig["unlock_ep"].asString().toIntOrNull() ?: 4
        }
    }


}