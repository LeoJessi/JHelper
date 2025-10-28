package top.jessi.jhelper.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.io.Reader
import java.lang.reflect.Type

object IGson {

    val gson: Gson by lazy { Gson() }

    /** JSON → 对象（指定 Class） */
    @JvmStatic
    fun <T> fromJson(json: String, clazz: Class<T>): T = gson.fromJson(json, clazz)

    /** JSON → 对象（指定 Type） */
    @JvmStatic
    fun <T> fromJson(json: String, type: Type): T = gson.fromJson(json, type)

    /** JSON → 对象（JsonReader + Type） */
    @JvmStatic
    fun <T> fromJson(reader: JsonReader, type: Type): T = gson.fromJson(reader, type)

    /** JSON → 对象（Reader + Class） */
    @JvmStatic
    fun <T> fromJson(reader: Reader, clazz: Class<T>): T = gson.fromJson(reader, clazz)

    /** JSON → 对象（Reader + Type） */
    @JvmStatic
    fun <T> fromJson(reader: Reader, type: Type): T = gson.fromJson(reader, type)

    /** JSON → List<T> */
    @JvmStatic
    fun <T> fromJsonToList(json: String, clazz: Class<T>): List<T> {
        val type = TypeToken.getParameterized(List::class.java, clazz).type
        return gson.fromJson(json, type)
    }

    @JvmStatic
    fun <K, V> fromJsonToMap(json: String): Map<K, V> {
        val type = object : TypeToken<LinkedHashMap<K, V>>() {}.type
        return gson.fromJson(json, type)
    }

    /** ✅ Kotlin专用: 支持复杂泛型推断 */
    inline fun <reified T> fromJson(json: String): T {
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(json, type)
    }

    /** 对象 → JSON 字符串 */
    @JvmStatic
    fun toJson(src: Any?): String = gson.toJson(src)

    /** 对象 → JSON 字符串（指定类型） */
    @JvmStatic
    fun toJson(src: Any?, type: Type): String = gson.toJson(src, type)
}