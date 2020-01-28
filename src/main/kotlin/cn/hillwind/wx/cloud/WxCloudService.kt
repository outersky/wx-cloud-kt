package cn.hillwind.wx.cloud

import me.chanjar.weixin.common.error.WxErrorException

/**
 * 云开发服务
 */
interface WxCloudService {

    /**
     * 获取云数据库服务
     */
    fun dbService(): WxCloudDbService

    /**
     * 获取云存储服务
     */
    fun storageService(): WxCloudStorageService

    /**
     * 获取云函数服务
     */
    fun functionService(): WxCloudFunctionService

    /**
     * 进行相应的公众号切换.
     *
     * @param mpId 公众号标识
     * @return 切换是否成功
     */
    fun switchover(mpId: String?): Boolean

    /**
     * 进行云开发环境切换.
     *
     * @param envName 环境名称
     * @return 切换是否成功
     */
    fun switchoverEnv(envName: String): Boolean

}

/**
 * 云数据库服务
 */
interface WxCloudDbService {
    /**
     * 查询
     */
    @Throws(WxErrorException::class)
    fun query(wxCloudDbQuery: WxCloudDbQuery): WxCloudDbQueryResult

    /**
     * 计数
     */
    @Throws(WxErrorException::class)
    fun count(wxCloudDbQuery: WxCloudDbQuery): WxCloudDbCountResult

    /**
     * 新增
     */
    @Throws(WxErrorException::class)
    fun add(wxCloudDbQuery: WxCloudDbQuery): WxCloudDbAddResult

    /**
     * 更新
     */
    @Throws(WxErrorException::class)
    fun update(wxCloudDbQuery: WxCloudDbQuery): WxCloudDbUpdateResult

    /**
     * 查询列表
     * @param collectionName 集合名称
     * @param cls 返回的实体类型
     * @param criteriaJson 查询条件
     * @return 实体列表
     */
    @Throws(WxErrorException::class)
    fun <T : Any> findList(collectionName: String, cls: Class<T>, criteriaJson: String = ""): List<T>

    /**
     * 根据 ID 查询单个实体对象
     * @return 实体对象，没有找到返回null
     */
    @Throws(WxErrorException::class)
    fun <T : Any> findById(collectionName: String, id: String, cls: Class<T>): T?

}

/**
 * 云存储服务
 */
interface WxCloudStorageService {

    /**
     * 获取文件上传的URL
     *  @param path 云存储文件保存路径 "test/abcdefg.jpg" 不能有前导的/ , 否则会报无效路径的错误
     */
    @Throws(WxErrorException::class)
    fun getUploadUrl(path: String): WxCloudStorageUploadResult

    /**
     * 获取下载URL
     * @param fileIds 要下载的文件ID， 形如 cloud:// 的格式
     */
    @Throws(WxErrorException::class)
    fun getDownloadUrls(maxAgeSecond: Int, fileIds: List<String>): WxCloudStorageDownloadResult

    /**
     * 删除文件
     * @param fileIds 要删除的文件ID，形如 cloud:// 的格式
     */
    @Throws(WxErrorException::class)
    fun delete(fileIds: List<String>): WxCloudStorageDeleteResult
}

/**
 * 云函数服务
 */
interface WxCloudFunctionService {

    /**
     * 调用
     * @param name 云函数名称
     * @param paramsJson 调用参数
     * @return 执行结果
     */
    @Throws(WxErrorException::class)
    fun invoke(name: String, paramsJson: String? = null): WxCloudFunctionInvokeResult
}