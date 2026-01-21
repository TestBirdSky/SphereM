package com.sphere.shortvideos.helper

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.notification.NotificationHelper

/**
 * Date：2026/1/20
 * Describe: Firebase 远程配置助手
 */
object OtherHelper {
    private var topicSuccessName by MMKVData("")

    private val topicBr = arrayOf("drama_br_data_fcm", "drama_br_normal_fcm")
    private val topicId = arrayOf("drama_id_data_fcm", "drama_id_normal_fcm")

    var isNeedFetch = true

    fun registerInfo(context: Context) {
        NotificationHelper.registerScreenUnlockReceiver(context)
        if (LauageTools.isIndonesia()) {
            topicId.forEach {
                register(it)
            }
        } else {
            topicBr.forEach {
                register(it)
            }
        }
    }




    private fun register(topicName: String) {
        if (topicSuccessName.contains(topicName)) return
        runCatching {
            Firebase.messaging.subscribeToTopic(topicName).addOnSuccessListener {
                topicSuccessName += topicName
            }
        }
    }
}