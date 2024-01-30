/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.savedstate

@Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")
actual class Bundle {
    private val mMap: MutableMap<String?, Any?>
    actual constructor() { mMap = LinkedHashMap() }
    actual constructor(initialCapacity: Int) { mMap = LinkedHashMap(initialCapacity) }
    actual constructor(bundle: Bundle) { mMap = LinkedHashMap(bundle.mMap) }

    actual fun size() = mMap.size
    actual fun isEmpty() = mMap.isEmpty()
    actual fun clear() = mMap.clear()
    actual fun containsKey(key: String?) = mMap.containsKey(key)
    actual fun remove(key: String?) { mMap.remove(key) }
    actual fun keySet(): Set<String?> = mMap.keys
    actual fun putAll(bundle: Bundle) { mMap.putAll(bundle.mMap) }

    actual fun putByte(key: String?, value: Byte) { mMap.put(key, value) }
    actual fun putChar(key: String?, value: Char) { mMap.put(key, value) }
    actual fun putShort(key: String?, value: Short) { mMap.put(key, value) }
    actual fun putInt(key: String?, value: Int) { mMap.put(key, value) }
    actual fun putLong(key: String?, value: Long) { mMap.put(key, value) }
    actual fun putFloat(key: String?, value: Float) { mMap.put(key, value) }
    actual fun putDouble(key: String?, value: Double) { mMap.put(key, value) }
    actual fun putString(key: String?, value: String?) { mMap.put(key, value) }
    actual fun putCharSequence(key: String?, value: CharSequence?) { mMap.put(key, value) }
    actual fun putIntegerArrayList(key: String?, value: ArrayList<Int>?) { mMap.put(key, value) }
    actual fun putStringArrayList(key: String?, value: ArrayList<String>?) { mMap.put(key, value) }
    actual fun putByteArray(key: String?, value: ByteArray?) { mMap.put(key, value) }
    actual fun putBundle(key: String?, value: Bundle?) { mMap.put(key, value) }

    actual fun getByte(key: String?): Byte = getByte(key, defaultValue = 0)
    actual fun getByte(key: String?, defaultValue: Byte): Byte = getObject(key, defaultValue)
    actual fun getChar(key: String?): Char = getChar(key, defaultValue = 0.toChar())
    actual fun getChar(key: String?, defaultValue: Char): Char = getObject(key, defaultValue)
    actual fun getShort(key: String?): Short = getShort(key, defaultValue = 0)
    actual fun getShort(key: String?, defaultValue: Short): Short = getObject(key, defaultValue)
    actual fun getInt(key: String?): Int = getInt(key, defaultValue = 0)
    actual fun getInt(key: String?, defaultValue: Int): Int = getObject(key, defaultValue)
    actual fun getLong(key: String?): Long = getLong(key, defaultValue = 0L)
    actual fun getLong(key: String?, defaultValue: Long): Long = getObject(key, defaultValue)
    actual fun getFloat(key: String?): Float = getFloat(key, defaultValue = 0f)
    actual fun getFloat(key: String?, defaultValue: Float): Float = getObject(key, defaultValue)
    actual fun getDouble(key: String?): Double = getDouble(key, defaultValue = 0.0)
    actual fun getDouble(key: String?, defaultValue: Double): Double = getObject(key, defaultValue)
    actual fun getString(key: String?): String? = getObject(key)
    actual fun getString(key: String?, defaultValue: String): String =
        getString(key) ?: defaultValue
    actual fun getCharSequence(key: String?): CharSequence? = getObject(key)
    actual fun getCharSequence(key: String?, defaultValue: CharSequence): CharSequence =
        getCharSequence(key) ?: defaultValue
    actual fun getIntegerArrayList(key: String?): ArrayList<Int>? = getArrayList(key)
    actual fun getStringArrayList(key: String?): ArrayList<String>? = getArrayList(key)
    actual fun getByteArray(key: String?): ByteArray? = getObject(key)
    actual fun getBundle(key: String?): Bundle? = getObject(key)

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> getArrayList(key: String?): ArrayList<T>? {
        val o = mMap.get(key) ?: return null
        return try {
            o as ArrayList<T>?
        } catch (e: ClassCastException) {
            typeWarning(key, o, "ArrayList<" + T::class.simpleName!! + ">", e)
            null
        }
    }

    private inline fun <reified T> getObject(key: String?): T? {
        val o = mMap.get(key) ?: return null
        return try {
            o as T?
        } catch (e: ClassCastException) {
            typeWarning(key, o, T::class.simpleName!!, e)
            null
        }
    }

    private inline fun <reified T> getObject(key: String?, defaultValue: T): T {
        val o = mMap.get(key) ?: return defaultValue
        return try {
            o as T
        } catch (e: ClassCastException) {
            typeWarning(key, o, T::class.simpleName!!, defaultValue, e)
            defaultValue
        }
    }

    // Log a message if the value was non-null but not of the expected type
    private fun typeWarning(
        key: String?,
        value: Any?,
        className: String,
        defaultValue: Any?,
        e: RuntimeException
    ) {
        val sb = StringBuilder()
        sb.append("Key ")
        sb.append(key)
        sb.append(" expected ")
        sb.append(className)
        if (value != null) {
            sb.append(" but value was a ")
            sb.append(value::class.qualifiedName)
        } else {
            sb.append(" but value was of a different type")
        }
        sb.append(".  The default value ")
        sb.append(defaultValue)
        sb.append(" was returned.")
        println(sb.toString())
        println("Attempt to cast generated internal exception: $e")
    }

    private fun typeWarning(key: String?, value: Any?, className: String, e: RuntimeException) {
        typeWarning(key, value, className, "<null>", e)
    }
}
