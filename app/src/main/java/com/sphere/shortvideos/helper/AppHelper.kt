package com.sphere.shortvideos.helper

import android.content.Context
import android.util.Base64
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.notification.NotificationHelper
import java.nio.charset.StandardCharsets

/**
 * Date：2026/1/20
 * Describe: Firebase 远程配置助手
 */
object AppHelper {
    private var topicSuccessName by MMKVData("")

    private val topicBr = arrayOf("drama_br_data_fcm", "drama_br_normal_fcm")
    private val topicId = arrayOf("drama_id_data_fcm", "drama_id_normal_fcm")
    private val topicEn = arrayOf("drama_en_data_fcm", "drama_en_normal_fcm")

    var isIceLuncher = true

    fun registerInfo(context: Context) {
        NotificationHelper.registerScreenUnlockReceiver(context)
        if (LauageTools.isIndonesia()) {
            topicId.forEach {
                register(it)
            }
        } else if (LauageTools.isBrazil()) {
            topicBr.forEach {
                register(it)
            }
        } else {
            topicEn.forEach {
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

    fun decrypt(data: String, code: Int): String { // Base64 解码
        val decodedBytes = Base64.decode(data, Base64.DEFAULT)

        // 异或解密
        val xorList = ByteArray(decodedBytes.size)
        for (i in decodedBytes.indices) {
            xorList[i] = (decodedBytes[i].toInt() xor code).toByte()
        }

        // 转换为字符串
        return String(xorList, StandardCharsets.UTF_8)
    }

    fun encrypt(data: String, code: Int): String { // 加密：异或 + Base64 编码
        val bytes = data.toByteArray(StandardCharsets.UTF_8)
        
        // 异或加密
        val xorList = ByteArray(bytes.size)
        for (i in bytes.indices) {
            xorList[i] = (bytes[i].toInt() xor code).toByte()
        }
        
        // Base64 编码
        return Base64.encodeToString(xorList, Base64.DEFAULT)
    }
}