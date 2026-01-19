package com.sphere.shortvideos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sphere.shortvideos.database.DramaDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

lateinit var mApp: App
val database by lazy { DramaDatabase.buildInstance(mApp) }
val isDebugMode by lazy { true }
var unlockIndex = 3

fun logError(any: Any?) {
    if (isDebugMode) Log.e("Sphere", "$any")
}

fun Context.dp2Px(dp: Int) = run { (resources.displayMetrics.density * dp).toInt() }

fun Context.showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, msg, duration).apply {
        setGravity(Gravity.CENTER, 0, 0)
    }.show()
}

fun Context.showToast(msg: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, getString(msg), duration).apply {
        setGravity(Gravity.CENTER, 0, 0)
    }.show()
}

fun startFlowTicker(first: Long, interval: Long) = flow {
    delay(first)
    while (true) {
        emit(Unit)
        delay(interval)
    }
}

fun HashMap<String, Any?>.toBundle(): Bundle? {
    if (isNullOrEmpty()) return null
    val bundle = Bundle().apply {
        forEach { (t, u) ->
            when (u) {
                is String -> putString(t, u)
                is Int -> putInt(t, u)
                is Long -> putLong(t, u)
                is Float -> putFloat(t, u)
                is Double -> putDouble(t, u)
                is Boolean -> putBoolean(t, u)
                else -> Unit
            }
        }
    }
    return bundle
}

fun isInteractive() = runCatching { (mApp.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive }.getOrNull() ?: false

inline fun <reified T> Context.nextView(block: Intent.() -> Unit = {}) {
    startActivity(Intent(this, T::class.java).also { it.block() })
}

inline fun <reified T> T.toJson(): String {
    return Gson().toJson(this)
}

inline fun <reified T> String.fromJson(): T {
    val type = object : TypeToken<T>() {}.type
    return Gson().fromJson(this, type)
}