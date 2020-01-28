package cn.hillwind.wx.cloud

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * @author someone
 */
object WxCloudGsonBuilder {
    private val INSTANCE = GsonBuilder()
    fun create(): Gson {
        return INSTANCE.create()
    }

    init {
        INSTANCE.disableHtmlEscaping()
//        INSTANCE.registerTypeAdapter(WxCloudDbQueryResult::class.java, WxCloudDbQueryResultAdapter())
//        INSTANCE.registerTypeAdapter(WxCloudDbUpdateResult::class.java, WxCloudDbUpdateResultAdapter())
    }
}