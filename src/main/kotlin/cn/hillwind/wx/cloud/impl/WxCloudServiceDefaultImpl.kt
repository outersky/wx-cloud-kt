package cn.hillwind.wx.cloud.impl

import cn.hillwind.wx.cloud.*
import me.chanjar.weixin.common.WxType
import me.chanjar.weixin.common.error.WxError
import me.chanjar.weixin.common.error.WxErrorException
import me.chanjar.weixin.common.util.DataUtils
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder
import me.chanjar.weixin.common.util.http.apache.Utf8ResponseHandler
import me.chanjar.weixin.mp.api.WxMpService
import org.apache.http.Consts
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.slf4j.LoggerFactory
import java.io.IOException

class WxCloudServiceDefaultImpl(val wxMpService: WxMpService, val cloudProperties: WxCloudProperties) : WxCloudDbService, WxCloudStorageService, WxCloudFunctionService {

    private val log = LoggerFactory.getLogger(WxCloudServiceDefaultImpl::class.java)

    private val httpClient: CloseableHttpClient
        get() = DefaultApacheHttpClientBuilder.get().build()

    @Throws(WxErrorException::class)
    override fun query(wxCloudDbQuery: WxCloudDbQuery): WxCloudDbQueryResult {
        return innerImpl(wxCloudDbQuery, WxCloudApiUrl.Database.QUERY, WxCloudDbQueryResult::class.java)
    }

    @Throws(WxErrorException::class)
    private fun <T : WxCloudResult> innerImpl(wxCloudDbQuery: WxCloudDbQuery, url: WxCloudApiUrl.Database, cls: Class<T>): T {
        val env = cloudProperties.getEnv()
        val postData = wxCloudDbQuery.toJsonString(env)
        val responseContent = this.post(url, postData)
        val result = WxCloudResult.fromJson(responseContent, cls)
        if (!result.isSuccess()) throw WxErrorException(WxError.fromJson(responseContent))
        return result
    }

    override fun count(wxCloudDbQuery: WxCloudDbQuery): WxCloudDbCountResult {
        return innerImpl(wxCloudDbQuery, WxCloudApiUrl.Database.COUNT, WxCloudDbCountResult::class.java)
    }

    override fun add(wxCloudDbQuery: WxCloudDbQuery): WxCloudDbAddResult {
        return innerImpl(wxCloudDbQuery, WxCloudApiUrl.Database.ADD, WxCloudDbAddResult::class.java)
    }

    override fun getUploadUrl(path: String): WxCloudStorageUploadResult {
        val env = cloudProperties.getEnv()
        val postData = """
            {
            	"env": "$env",
            	"path": "$path"
            }
        """.trimIndent()
        val responseContent = this.post(WxCloudApiUrl.Storage.UPLOAD, postData)
        val result = WxCloudResult.fromJson(responseContent, WxCloudStorageUploadResult::class.java)
        result.path = path
        if (!result.isSuccess()) throw WxErrorException(WxError.fromJson(responseContent))
        return result
    }

    override fun getDownloadUrls(maxAgeSecond: Int, fileIds: List<String>): WxCloudStorageDownloadResult {

        val env = cloudProperties.getEnv()
        val fileLists = fileIds.joinToString(",") {
            """
                {
                    "fileid":"$it",
                    "max_age":$maxAgeSecond
                }
            """
        }
        val postData = """
            {
            	"env": "$env",
            	"file_list": [ $fileLists ]
            }
        """.trimIndent()
        val responseContent = this.post(WxCloudApiUrl.Storage.DOWNLOAD, postData)
        val result = WxCloudResult.fromJson(responseContent, WxCloudStorageDownloadResult::class.java)
        if (!result.isSuccess()) throw WxErrorException(WxError.fromJson(responseContent))
        return result
    }

    override fun delete(fileIds: List<String>): WxCloudStorageDeleteResult {
        val env = cloudProperties.getEnv()
        val fileLists = fileIds.joinToString(",") { "\"$it\"" }
        val postData = """
            {
            	"env": "$env",
            	"fileid_list": [ $fileLists ]
            }
        """.trimIndent()
        val responseContent = this.post(WxCloudApiUrl.Storage.DELETE, postData)
        val result = WxCloudResult.fromJson(responseContent, WxCloudStorageDeleteResult::class.java)
        if (!result.isSuccess()) throw WxErrorException(WxError.fromJson(responseContent))
        return result
    }

    @Throws(WxErrorException::class)
    override fun invoke(name: String, paramsJson: String?): WxCloudFunctionInvokeResult {
        val env = cloudProperties.getEnv()
        val responseContent = this.post(WxCloudApiUrl.Function.INVOKE.getUrl() + "?env=$env&name=$name", paramsJson)
        val result = WxCloudResult.fromJson(responseContent, WxCloudFunctionInvokeResult::class.java)
        if (!result.isSuccess()) throw WxErrorException(WxError.fromJson(responseContent))
        return result
    }

    private fun doQuery(queryStr: String): WxCloudDbQueryResult {
        return query(WxCloudDbQuery(queryStr))
    }

    @Throws(WxErrorException::class)
    override fun update(wxCloudDbQuery: WxCloudDbQuery): WxCloudDbUpdateResult {
        return innerImpl(wxCloudDbQuery, WxCloudApiUrl.Database.UPDATE, WxCloudDbUpdateResult::class.java)
    }

    @Throws(WxErrorException::class)
    override fun <T : Any> findList(collectionName: String, cls: Class<T>, criteriaJson: String): List<T> {
        @Suppress("NAME_SHADOWING") var criteriaJson = criteriaJson.trim()
        if (criteriaJson.isNotEmpty() && !criteriaJson.startsWith(".")) {
            criteriaJson = ".$criteriaJson"
        }
        val queryStr = """db.collection("$collectionName")${criteriaJson}.get()"""
        val result = doQuery(queryStr).data
        if (result.isNullOrEmpty()) {
            return emptyList()
        }
        return result.map { JsonHelper.toBean(it, cls) }
    }

    override fun <T : Any> findById(collectionName: String, id: String, cls: Class<T>): T? {
        val queryStr = """db.collection("$collectionName").where({_id:'$id'}).limit(1).get()"""

        val result = doQuery(queryStr).data
        if (result.isNullOrEmpty()) {
            return null
        }
        return JsonHelper.toBean(result[0], cls)
    }

    @Throws(WxErrorException::class)
    private fun get(url: String, queryParam: String?): String? {
        return execute(Method.GET, url, queryParam)
    }

    @Throws(WxErrorException::class)
    private fun get(url: WxCloudApiUrl, queryParam: String?): String? {
        return this.get(url.getUrl(), queryParam)
    }

    @Throws(WxErrorException::class)
    fun post(url: String, postData: String?): String? {
        return execute(Method.POST, url, postData)
    }

    @Throws(WxErrorException::class)
    fun post(url: WxCloudApiUrl, postData: String?): String? {
        return this.post(url.getUrl(), postData)
    }

    enum class Method {
        GET, POST
    }

    /**
     * 向微信端发送请求，在这里执行的策略是当发生access_token过期时才去刷新，然后重新执行请求，而不是全局定时请求.
     */
    @Throws(WxErrorException::class)
    fun execute(method: Method, uri: String, data: String?): String? {
        var retryTimes = 0
        val maxRetryTimes = 1
        val retrySleepMillis = 50
        do {
            try {
                return executeInternal(method, uri, data)
            } catch (e: WxErrorException) {
                if (retryTimes + 1 > maxRetryTimes) {
                    log.warn("重试达到最大次数【{}】", maxRetryTimes)
                    throw RuntimeException("微信服务端异常，超出重试次数")
                }
                val error = e.error
                // -1 系统繁忙, 1000ms后重试
                if (error.errorCode == -1) {
                    val sleepMillis = retrySleepMillis * (1 shl retryTimes)
                    try {
                        log.warn("微信系统繁忙，{} ms 后重试(第{}次)", sleepMillis, retryTimes + 1)
                        Thread.sleep(sleepMillis.toLong())
                    } catch (e1: InterruptedException) {
                        throw RuntimeException(e1)
                    }
                } else {
                    throw e
                }
            }
        } while (retryTimes++ < maxRetryTimes)
        log.warn("重试达到最大次数【{}】", maxRetryTimes)
        throw RuntimeException("微信服务端异常，超出重试次数")
    }

    @Throws(WxErrorException::class)
    protected fun executeInternal(method: Method, uri: String, data: String?): String? {
        val dataForLog = DataUtils.handleDataWithSecret(data)
        require(!uri.contains("access_token=")) { "uri参数中不允许有access_token: $uri" }
        val accessToken = wxMpService.getAccessToken(false)
        val uriWithAccessToken = uri + (if (uri.contains("?")) "&" else "?") + "access_token=" + accessToken
        return try {
            val result: String
            result = if (method == Method.GET) {
                doGet(uriWithAccessToken, data, WxType.MP)
            } else {
                doPost(uriWithAccessToken, data, WxType.MP)
            }
            log.debug("\n【请求地址】: {}\n【请求参数】：{}\n【响应数据】：{}", uriWithAccessToken, dataForLog, result)
            result
        } catch (e: WxErrorException) {
            val error = e.error
            /*
             * 发生以下情况时尝试刷新access_token
             * 40001 获取access_token时AppSecret错误，或者access_token无效
             * 42001 access_token超时
             * 40014 不合法的access_token，请开发者认真比对access_token的有效性（如是否过期），或查看是否正在为恰当的公众号调用接口
             */
/*
            if (error.getErrorCode() == 42001 || error.getErrorCode() == 40001 || error.getErrorCode() == 40014) {
                // 强制设置wxMpConfigStorage它的access token过期了，这样在下一次请求里就会刷新access token
                this.getWxMpConfigStorage().expireAccessToken();
                if (this.getWxMpConfigStorage().autoRefreshToken()) {
                    return this.execute(executor, uri, data);
                }
            }
*/if (error.errorCode != 0) {
                log.error("\n【请求地址】: {}\n【请求参数】：{}\n【错误信息】：{}", uriWithAccessToken, dataForLog, error)
                throw WxErrorException(error, e)
            }
            null
        } catch (e: IOException) {
            log.error("\n【请求地址】: {}\n【请求参数】：{}\n【异常信息】：{}", uriWithAccessToken, dataForLog, e.message)
            throw WxErrorException(WxError.builder().errorMsg(e.message).build(), e)
        }
    }

    @Throws(WxErrorException::class, IOException::class)
    private fun doGet(uri: String, queryParam: String?, wxType: WxType): String {
        @Suppress("NAME_SHADOWING") var uri = uri
        if (queryParam != null) {
            if (uri.indexOf('?') == -1) {
                uri += '?'
            }
            uri += if (uri.endsWith("?")) queryParam else "&$queryParam"
        }
        val httpGet = HttpGet(uri)
        try {
            httpClient.execute(httpGet).use { response ->
                val responseContent = Utf8ResponseHandler.INSTANCE.handleResponse(response)
                val error = WxError.fromJson(responseContent, wxType)
                if (error.errorCode != 0) {
                    throw WxErrorException(error)
                }
                return responseContent
            }
        } finally {
            httpGet.releaseConnection()
        }
    }

    @Throws(WxErrorException::class, IOException::class)
    private fun doPost(uri: String, postEntity: String?, wxType: WxType): String {
        val httpPost = HttpPost(uri)
        if (postEntity != null) {
            val entity = StringEntity(postEntity, Consts.UTF_8)
            httpPost.entity = entity
        }
        try {
            httpClient.execute(httpPost).use { response ->
                val responseContent = Utf8ResponseHandler.INSTANCE.handleResponse(response)
                if (responseContent.isEmpty()) {
                    throw WxErrorException(WxError.builder().errorCode(9999).errorMsg("无响应内容").build())
                }
                if (responseContent.startsWith("<xml>")) { //xml格式输出直接返回
                    return responseContent
                }
                val error = WxError.fromJson(responseContent, wxType)
                if (error.errorCode != 0) {
                    throw WxErrorException(error)
                }
                return responseContent
            }
        } finally {
            httpPost.releaseConnection()
        }
    }

}