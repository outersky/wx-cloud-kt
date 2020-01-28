package cn.hillwind.wx.cloud

/**
 * 微信接口地址域名部分的自定义设置信息.
 */
class WxCloudHostConfig {
    /**
     * 对应于：https://api.weixin.qq.com
     */
    var apiHost: String = ""

    companion object {
        const val API_DEFAULT_HOST_URL = "https://api.weixin.qq.com"
        fun buildUrl(prefix: String, path: String): String {
            return if (prefix == API_DEFAULT_HOST_URL) {
                API_DEFAULT_HOST_URL + path
            } else API_DEFAULT_HOST_URL + prefix + path
        }
    }
}