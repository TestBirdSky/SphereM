package com.sphere.shortvideos.helper.mmkv

import com.tencent.mmkv.MMKV
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val mmkvIns: MMKV by lazy { MMKV.mmkvWithID("sphere_data") }

class MMKVData<T>(
    private val default: T,
    private val mmkv: MMKV = mmkvIns
) : ReadWriteProperty<Any?, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return when (default) {
            is Int -> mmkv.decodeInt(property.name, default)
            is Long -> mmkv.decodeLong(property.name, default)
            is Float -> mmkv.decodeFloat(property.name, default)
            is Boolean -> mmkv.decodeBool(property.name, default)
            is Double -> mmkv.decodeDouble(property.name, default)
            is String -> mmkv.decodeString(property.name, default)
            is ByteArray -> mmkv.decodeBytes(property.name, default)
            else -> default
        } as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        when (value) {
            is Int -> mmkv.encode(property.name, value)
            is Long -> mmkv.encode(property.name, value)
            is Float -> mmkv.encode(property.name, value)
            is Boolean -> mmkv.encode(property.name, value)
            is Double -> mmkv.encode(property.name, value)
            is String -> mmkv.encode(property.name, value)
            is ByteArray -> mmkv.encode(property.name, value)
            else -> Unit
        }
    }
}