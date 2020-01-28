package cn.hillwind.wx.cloud

/**
 * 云开发的上下文环境
 */
class WxCloudContext {

    companion object {

        /**
         * 获取当前上下文的WxCloudService实例
         */
        fun getWxCloudService(): WxCloudService {
            return holder.getWxCloudService()
        }

        fun register(wxCloudServiceHolder: WxCloudServiceHolder) {
            holder = wxCloudServiceHolder
        }

        lateinit var holder: WxCloudServiceHolder

        fun <T> with(wxCloudService: WxCloudService, f: () -> T): T {
            holder.set(wxCloudService)
            try {
                return f()
            } finally {
                holder.remove()
            }
        }
    }

}

interface WxCloudServiceHolder {
    fun getWxCloudService(): WxCloudService
    fun set(service: WxCloudService)
    fun remove()
}

/**
 * 缺省实现，基于ThreadLocal
 */
class ThreadLocalHolder : WxCloudServiceHolder {

    override fun getWxCloudService(): WxCloudService {
        return wxCloudServiceHolder.get()
    }

    private val wxCloudServiceHolder = ThreadLocal<WxCloudService>()

    override fun set(service: WxCloudService) {
        wxCloudServiceHolder.set(service)
    }

    override fun remove() {
        wxCloudServiceHolder.remove()
    }

}