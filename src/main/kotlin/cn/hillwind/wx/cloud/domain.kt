package cn.hillwind.wx.cloud

import com.google.gson.annotations.SerializedName
import me.chanjar.weixin.common.error.WxErrorException
import java.util.*

/**
 * 微信云开发数据库标注类
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CloudDb(val value: String)

//实体或者字段的备注
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
annotation class DbComment(val value: String)

// 云数据库的基础类
abstract class CloudBase {

    @SerializedName("_id")
    var id: String = ""

    @SerializedName("_openid")
    var openid: String = ""

    fun toJson(): String {
        return JsonHelper.toJson(this)
    }

    // 集合名称
    fun collectionName() = javaClass.getAnnotation(CloudDb::class.java)!!.value

    //单个对象输出成json格式
    abstract fun innerDataJson(): String

    //整体更新实体。注意只有innerDataJson()中的字段才生效
    @Throws(WxErrorException::class)
    fun update(): WxCloudDbUpdateResult {
        return update(innerDataJson())
    }

    //更新dataJson中指定的属性
    @Throws(WxErrorException::class)
    fun update(dataJson: String): WxCloudDbUpdateResult {
        return util.update(collectionName(), id, dataJson)
    }

    //实体整体保存到数据库。注意只有innerDataJson()中的字段才生效
    fun add(): WxCloudDbAddResult {
        return add(innerDataJson())
    }

    //保存到数据库。
    @Throws(WxErrorException::class)
    fun add(dataJson: String): WxCloudDbAddResult {
        val result = util.add(collectionName(), dataJson)
        id = result.idList!![0]
        return result
    }

}

internal object util {

    fun <T : Any> parse(json: String, cls: Class<T>): T {
        return JsonHelper.toBean(json, cls)
    }

    /**
     * 更新数据库
     * @param collectionName 集合名称
     * @param id 要更新的数据id
     * @param dataJson 要更新的内容
     */
    @Throws(WxErrorException::class)
    fun update(collectionName: String, id: String, dataJson: String): WxCloudDbUpdateResult {
        if (dataJson.trim().isEmpty()) throw RuntimeException("dataJson should not be null or empty.")

        val updateStr = """
                db.collection("$collectionName").doc("$id").update({
                                data: $dataJson
                            })
            """.trimIndent()
        val wxCloudDbService = WxCloudContext.getWxCloudService().dbService()
        return wxCloudDbService.update(WxCloudDbQuery(updateStr))
    }

    /**
     * 增加数据库记录
     * @param collectionName 集合名称
     * @param dataJson 数据内容
     */
    @Throws(WxErrorException::class)
    fun add(collectionName: String, dataJson: String): WxCloudDbAddResult {
        if (dataJson.trim().isEmpty()) throw RuntimeException("dataJson should not be null or empty.")

        val str = """
                db.collection("$collectionName").add({
                                data: $dataJson
                            })
            """.trimIndent()
        val wxCloudDbService = WxCloudContext.getWxCloudService().dbService()
        return wxCloudDbService.add(WxCloudDbQuery(str))
    }

    /**
     * 查询数据库.
     * @param collectionName 集合名称
     * @param criteriaJson 查询条件
     * @param cls 查询结果的类型，会自动进行对象的组装
     */
    @Throws(WxErrorException::class)
    fun <T : Any> findList(collectionName: String, criteriaJson: String = "", cls: Class<T>): List<T> {
        val wxCloudDbService = WxCloudContext.getWxCloudService().dbService()
        @Suppress("NAME_SHADOWING") var criteriaJson = criteriaJson.trim()
        if (criteriaJson.isNotEmpty() && !criteriaJson.startsWith(".")) {
            criteriaJson = ".$criteriaJson"
        }
        val queryStr = """db.collection("$collectionName") $criteriaJson .get()"""

        val queryResult: WxCloudDbQueryResult = wxCloudDbService.query(WxCloudDbQuery(queryStr))
        if (queryResult.errcode != 0 || queryResult.data!!.isEmpty()) {
            return emptyList()
        }

        return queryResult.data!!.map { parse(it, cls) }
    }

    /**
     * 根据ID查找
     * @param collectionName 集合名称
     * @param id 记录ID
     * @param cls 查询结果的类型，会自动进行对象的组装
     */
    @Throws(WxErrorException::class)
    fun <T : Any> find(collectionName: String, id: String, cls: Class<T>): T? {
        val list = findList(collectionName, """.where({_id:"$id"}).limit(1)""", cls)
        if (list.isNullOrEmpty()) return null
        return list[0]
    }
}

//数据库静态增强工具
open class CloudStatic<T : CloudBase>(val cls: Class<T>) {
    val collectionName = cls.getAnnotation(CloudDb::class.java)!!.value

    /**
     * 解析json成对象
     */
    fun parse(json: String): T {
        return util.parse(json, cls)
    }

    /**
     * 更新记录
     * @param id 记录ID
     * @param dataJson 更新内容
     */
    @Throws(WxErrorException::class)
    fun update(id: String, dataJson: String): WxCloudDbUpdateResult {
        return util.update(collectionName, id, dataJson)
    }

    /**
     * 更新记录。
     * 整体更新，注意只有innerDataJson()里面的内容才生效
     */
    @Throws(WxErrorException::class)
    fun update(obj: T): WxCloudDbUpdateResult {
        return util.update(collectionName, obj.id, obj.innerDataJson())
    }

    /**
     * 保存新的一条数据
     * @param dataJson 数据内容
     */
    @Throws(WxErrorException::class)
    fun add(dataJson: String): WxCloudDbAddResult {
        return util.add(collectionName, dataJson)
    }

    /**
     * 保存一个实体对象
     * @param obj 实体， 会调用实体的innerDataJson()保存数据
     */
    @Throws(WxErrorException::class)
    fun add(obj: T): WxCloudDbAddResult {
        return util.add(collectionName, obj.innerDataJson())
    }

    /**
     * 保存一个或多个实体
     * @param objs 实体对象
     */
    @Throws(WxErrorException::class)
    fun addAll(vararg objs: T): WxCloudDbAddResult {
        return util.add(collectionName, """
            [
                ${objs.joinToString(",") { it.innerDataJson() }}
            ]
        """.trimIndent())
    }

    /**
     * 根据条件查找实体对象列表
     * @param criteriaJson 查询条件
     */
    @Throws(WxErrorException::class)
    fun findList(criteriaJson: String = ""): List<T> {
        return util.findList(collectionName, criteriaJson, cls)
    }

    /**
     * 根据ID查找单个实体对象
     * @param id 数据ID
     */
    @Throws(WxErrorException::class)
    fun find(id: String): T? {
        return util.find(collectionName, id, cls)
    }
}

@DbComment("服务器时间")
class ServerData {

    @SerializedName("\$date")
    var date: Long = 0L

    val dateTime: Date
        get() = Date(date)

    override fun toString(): String {
        return dateTime.longFormat()
    }
}
