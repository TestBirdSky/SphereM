package com.sphere.shortvideos.helper

import android.os.Build
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.mApp
import com.sphere.shortvideos.startFlowTicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object InstallReferrerManager {

    private var tickerJob: Job? = null

    fun fetch() {
        if (MMKVRepository.referrerUrl.isNotEmpty()) return
        tickerJob = CoroutineScope(Dispatchers.IO).launch {
            startFlowTicker(1000L, 30000L).collect {
                runCatching {
                    val referrerClient = InstallReferrerClient.newBuilder(mApp).build()
                    referrerClient.startConnection(object : InstallReferrerStateListener {
                        override fun onInstallReferrerSetupFinished(responseCode: Int) {
                            runCatching {
                                if (InstallReferrerClient.InstallReferrerResponse.OK == responseCode) {
                                    tickerJob?.cancel()
                                    val referrerDetails = referrerClient.installReferrer
                                    referrerDetails?.installReferrer?.let { MMKVRepository.referrerUrl = it }
                                    install { obj ->
                                        obj.run {
                                            put("filthy", Build.ID ?: "")
                                            put("gate", referrerDetails?.installReferrer ?: "")
                                            put("drank", referrerDetails?.installVersion ?: "")
                                            put("crepe", "")
                                            put("awl", "osseous")
                                            put("cudgel", referrerDetails?.referrerClickTimestampSeconds ?: 0L)
                                            put("enamel", referrerDetails?.installBeginTimestampSeconds ?: 0L)
                                            put("catskill", referrerDetails?.referrerClickTimestampServerSeconds ?: 0L)
                                            put("monk", referrerDetails?.installBeginTimestampServerSeconds ?: 0L)
                                            put("marshall", 0L)
                                            put("lichen", 0L)
                                        }
                                    }
                                }
                                referrerClient.endConnection()
                            }
                        }

                        override fun onInstallReferrerServiceDisconnected() = Unit
                    })
                }
            }
        }
    }


}