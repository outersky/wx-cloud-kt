package cn.hillwind.wx.cloud

import java.text.SimpleDateFormat
import java.util.*

/**
 * 将
 */
fun <T : Any> T?.mapToString(f: (T) -> String): String {
    if (this == null) return ""
    return f(this)
}

fun String.ensurePrefix(prefix: String): String {
    return if (this.startsWith(prefix)) this else prefix + this
}

private val longFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
private val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
private val timeFormatter = SimpleDateFormat("yyyy-MM-dd")

/**
 * 输出年月日时分秒格式
 */
fun Date.longFormat(): String {
    return longFormatter.format(this)
}

/**
 * 输出年月日格式
 */
fun Date.dateFormat(): String {
    return dateFormatter.format(this)
}

/**
 * 输出时分秒格式
 */
fun Date.timeFormat(): String {
    return timeFormatter.format(this)
}
