package cn.hillwind.wx.cloud

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement

/**
 */
object JsonHelper {
    val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

    fun toJson(obj: Any?): String {
        return gson.toJson(obj)
    }

    fun <T> toBean(json: String, cls: Class<T>): T {
        return gson.fromJson(json, cls)
    }

}

/**
 * 快捷方法，如果对象不为空，就执行一个方法
 */
fun JsonElement?.ifNotJsonNull(f: (JsonElement) -> Unit) {
    this ?: return
    if (!isJsonNull) {
        f(this)
    }
}