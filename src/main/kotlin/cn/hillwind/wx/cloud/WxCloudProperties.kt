package cn.hillwind.wx.cloud

/**
 * wechat cloud properties
 *
 */
open class WxCloudProperties {

    var configs: List<CloudConfig>? = null

    class CloudConfig {
        /**
         * 设置env名称
         */
        var env: String = ""

    }

    // 默认的env名称
    var defaultEnv: String? = null

    @Transient
    private var currentEnv: String? = null

    // 获取当前环境名称
    fun getEnv(): String {
        return currentEnv ?: defaultEnv ?: configs!![0].env
    }

    /**
     * 切换环境
     */
    fun switchTo(envName: String): Boolean {
        var found = false
        configs!!.find { it.env == envName }?.also {
            currentEnv = envName
            found = true
        }
        return found
    }

    override fun toString(): String {
        return JsonHelper.toJson(this)
    }
}