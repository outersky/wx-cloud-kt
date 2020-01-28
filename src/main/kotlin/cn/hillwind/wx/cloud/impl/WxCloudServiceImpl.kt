package cn.hillwind.wx.cloud.impl

import cn.hillwind.wx.cloud.*
import me.chanjar.weixin.mp.api.WxMpService

class WxCloudServiceImpl(val wxMpService: WxMpService, val cloudProperties: WxCloudProperties) : WxCloudService {

    private val impl = WxCloudServiceDefaultImpl(wxMpService, cloudProperties)

    override fun dbService(): WxCloudDbService {
        return impl
    }

    override fun storageService(): WxCloudStorageService {
        return impl
    }

    override fun functionService(): WxCloudFunctionService {
        return impl
    }

    override fun switchover(mpId: String?): Boolean {
        return wxMpService.switchover(mpId)
    }

    override fun switchoverEnv(envName: String): Boolean {
        return cloudProperties.switchTo(envName)
    }

}
