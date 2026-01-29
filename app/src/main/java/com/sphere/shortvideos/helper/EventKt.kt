package com.sphere.shortvideos.helper

import com.google.firebase.analytics.FirebaseAnalytics
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import com.sphere.shortvideos.toBundle
import org.json.JSONObject

private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(mApp) }

fun firebaseEvent(eventName: String, params: HashMap<String, Any?> = hashMapOf()) {
    if (!isDebugMode) firebaseAnalytics.logEvent(eventName, params.toBundle()) else logError("$eventName, $params")
    localEvent(eventName, params)
}

fun install(block: (JSONObject) -> Unit) {
    val root = EventData.buildBody().apply {
        put("calypso", "vamp")
    }
    block.invoke(root)
    EventData.eventCall(root, success = {
        MMKVRepository.installJson = ""
    })
}

fun session() {
    val root = EventData.buildBody().apply {
        put("calypso", "appetite")
    }
    EventData.eventCall(root)
}

fun adImpression(obj: JSONObject) {
    val root = EventData.buildBody().apply {
        put("pat", obj)
    }
    EventData.eventCall(root)
}

fun localEvent(eventName: String, params: HashMap<String, Any?> = hashMapOf()) {
    val root = EventData.buildBody().apply {
        put("calypso", eventName)
    }
    root.put(eventName, JSONObject().apply {
        params.forEach { entry -> put(entry.key, entry.value) }
    })
    EventData.eventCall(root)
}

