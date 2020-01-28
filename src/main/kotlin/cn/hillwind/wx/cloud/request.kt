package cn.hillwind.wx.cloud

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.chanjar.weixin.common.error.WxError
import me.chanjar.weixin.common.error.WxErrorException
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.Serializable
import java.nio.charset.Charset
import java.util.*


/**
 * 云平台返回结果
 *
{
"errcode": 0,
"errmsg": "ok",
...
}
 *
 */
abstract class WxCloudResult : Serializable {
    var errcode: Int? = null
    var errmsg: String? = null

    fun isSuccess() = errcode == 0

    fun toJson() = WxCloudGsonBuilder.create().toJson(this)

    companion object {
        private const val serialVersionUID = 1L

        @Throws(WxErrorException::class)
        fun <T : WxCloudResult> fromJson(json: String?, cls: Class<T>): T {
            val result = WxCloudGsonBuilder.create().fromJson(json, cls)
            if (!result.isSuccess()) throw WxErrorException(WxError.fromJson(json))
            return result
        }
    }
}

/**
 * 数据库ADD结果
 *
{
"errcode": 0,
"errmsg": "ok",
"id_list": [
"be62d9c4-43ec-4dc6-8ca1-30b206eeed24",
"0f4b8add5cdd728a003bf5c83ed99dff"
]
}
 *
 */
class WxCloudDbAddResult : WxCloudResult() {

    @SerializedName("id_list")
    var idList: List<String>? = null

}


/**
 * 数据库Count结果
 */
class WxCloudDbCountResult : WxCloudResult() {
    var count: Int? = null
}


/**
 */
class WxCloudDbQuery(val query: String) : Serializable {

    fun toJsonString(env: String?): String {
        val map: MutableMap<String, Any?> = HashMap()
        map["query"] = query
        map["env"] = env
        return Gson().toJson(map)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}


/**
 * 数据库查询结果
 */
class WxCloudDbQueryResult : WxCloudResult() {
    var pager: Pager? = null
    var data: List<String>? = null

    class Pager : Serializable {
        @SerializedName("Offset")
        var offset: Int? = null
        @SerializedName("Limit")
        var limit: Int? = null
        @SerializedName("Total")
        var total: Int? = null

        companion object {
            private const val serialVersionUID = 1L
        }
    }

}


/**
 * 数据库查询结果
 *
 * {
 * "errcode": 0,
 * "errmsg": "ok",
 * "matched": 1,
 * "modified": 1,
 * "id": ""
 * }
 *
 */
class WxCloudDbUpdateResult : WxCloudResult() {
    var matched: Int? = null
    var modified: Int? = null
    var id: String? = null
}


/**
 * 获取文件下载路径的结果
 */
class WxCloudStorageDownloadResult : WxCloudResult() {
    @SerializedName("file_list")
    var fileList: List<FileInfo>? = null

    class FileInfo : Serializable {
        @SerializedName("fileid")
        var fileId: String? = null
        @SerializedName("download_url")
        var downloadUrl: String? = null
        @SerializedName("status")
        var status: Int? = null
        @SerializedName("errmsg")
        var errmsg: String? = null

        fun download(os: OutputStream) {
            val httpClient = DefaultApacheHttpClientBuilder.get().build()
            val httpGet = HttpGet(downloadUrl)
            try {
                httpClient.execute(httpGet).use { response ->
                    response.entity.writeTo(os)
                }
            } finally {
                httpGet.releaseConnection()
            }
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

}


/**
 * 文件删除结果
{
"errcode": 0,
"errmsg": "ok",
"delete_list": [
{
"fileid": "cloud://test2-4a89da.7465-test2-4a89da/A.png",
"status": 0,
"errmsg": "ok"
}
]
}
 */
class WxCloudStorageDeleteResult : WxCloudResult() {
    @SerializedName("delete_list")
    var deleteList: List<FileInfo>? = null

    class FileInfo : Serializable {
        @SerializedName("fileid")
        var fileId: String? = null
        @SerializedName("status")
        var status: Int? = null
        @SerializedName("errmsg")
        var errmsg: String? = null

        companion object {
            private const val serialVersionUID = 1L
        }
    }

}

/**
 * 获取文件上传路径的结果
 *
上传链接使用说明
用户获取到返回数据后，需拼装一个 HTTP POST 请求，其中 url 为返回包的 url 字段，Body 部分格式为 multipart/form-data，具体内容如下：

key	    value	    说明
key	    this/is/a/example/file.path	请求包中的 path 字段
Signature	q-sign-algorithm=sha1&q-ak=AKID9...	返回数据的 authorization 字段
x-cos-security-token	Cukha70zkXIBqkh1Oh...	返回数据的 token 字段
x-cos-meta-fileid	HDze32/qZENCwWi5...	返回数据的 cos_file_id 字段
file	文件内容	    文件的二进制内容

 */
class WxCloudStorageUploadResult : WxCloudResult() {
    //请求包中的path字段
    var path: String? = null

    @SerializedName("url")
    var url: String? = null

    @SerializedName("token")
    var token: String? = null

    @SerializedName("authorization")
    var authorization: String? = null

    @SerializedName("file_id")
    var fileId: String? = null

    @SerializedName("cos_file_id")
    var cosFileId: String? = null

    fun upload(localFile: File) {
        var httpClient: CloseableHttpClient? = null
        var response: CloseableHttpResponse? = null
        try {
            httpClient = HttpClients.createDefault()
            // 把一个普通参数和文件上传给下面这个地址 是一个servlet
            val httpPost = HttpPost(url)
            // 把文件转换成流对象FileBody
//            val bin = FileBody(localFile)
            val reqEntity = MultipartEntityBuilder.create() // 相当于<input type="file" name="file"/>
                    //下面两种方法都可以，但是file一定要放在最后！！！！！！否则会报MalformedPOSTRequest
                    .addTextBody("key", path)
                    .addTextBody("Signature", authorization)
                    .addTextBody("x-cos-security-token", token)
                    .addTextBody("x-cos-meta-fileid", cosFileId)
                    .addBinaryBody("file", localFile)
/*
                    .addPart("key", StringBody(path, ContentType.create("text/plain", Consts.UTF_8)))
                    .addPart("Signature", StringBody(authorization, ContentType.create("text/plain", Consts.UTF_8)))
                    .addPart("x-cos-security-token", StringBody(token, ContentType.create("text/plain", Consts.UTF_8)))
                    .addPart("x-cos-meta-fileid", StringBody(cosFileId, ContentType.create("text/plain", Consts.UTF_8)))
                    .addPart("file", bin) // 相当于<input type="text" name="userName" value=userName>
*/
                    .build()
            httpPost.entity = reqEntity
            // 发起请求 并返回请求的响应
            response = httpClient.execute(httpPost)
            // 获取响应对象
            val resEntity = response.entity
            if (resEntity != null) { // 打印响应长度
                println("Response content length: " + resEntity.contentLength)
                // 打印响应内容
                println(EntityUtils.toString(resEntity, Charset.forName("UTF-8")))
            }
            // 销毁
            EntityUtils.consume(resEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                response?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                httpClient?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}

/**
 * 云函数调用返回结果
 *
{
"errcode": 0,
"errmsg": "ok",
"resp_data": "{\"event\":{\"userInfo\":{\"appId\":\"SAMPLE_APPID\"}},\"appid\":\"SAMPLE_APPID\"}"
}
 */
class WxCloudFunctionInvokeResult : WxCloudResult() {
    @SerializedName("resp_data")
    var respData: String? = null
}