package cn.hillwind.wx.cloud

import kotlin.math.ceil

/*
db.collection('test')
  .where({
    //price: _.gt(10)
    name: /阿/,
    age: _.gt(10)
    //name: {$regex:'阿2.*'}
  })
  .field({
    name: true
  })
  .orderBy('price', 'desc')
 // .skip(1)
  .limit(10)
  .get()
 */

//分页支持

//数据库查询
class DbStatement<T : CloudBase>(private val cs: CloudStatic<T>) {
    val name: String = cs.collectionName
    var page: PageParam = PageParam()
    val conditions = mutableListOf<DbCondition>()
    var order: DbOrder? = null
    var fields = mutableListOf<String>()

    var skip: Int = 0
    var limit: Int = 10

    /**
     * 应用分页参数
     */
    fun apply(pageParam: PageParam): DbStatement<T> {
        limit = pageParam.pageSize
        skip = pageParam.pageSize * pageParam.pageNumber
        page = pageParam
        return this
    }

    /**
     * 执行查询
     */
    fun query(): PagedList<T> {
        val wxCloudDbService = WxCloudContext.getWxCloudService().dbService()
        val total = wxCloudDbService.count(WxCloudDbQuery(toCountSql())).count!!
        val queryResult = wxCloudDbService.query(WxCloudDbQuery(toQuerySql()))
        val list = queryResult.data!!.map { cs.parse(it) }
        return PagedList(list, page.pageSize, page.pageNumber, total)
    }

    /**
     * 输出查询SQL语句
     */
    fun toQuerySql(): String {
        return """
        ${toSql0()}
        .skip($skip)
        .limit(${limit})
        .get()
        """.trimIndent()
    }

    private fun toSql0(): String {
        return """
            db.collection('$name')
        ${if (conditions.isNotEmpty()) {
            ".where({ " + conditions.joinToString(",") { "${it.name} : ${it.oper}" } + " })"
        } else {
            ""
        }}
        ${if (fields.isNotEmpty()) {
            ".field({ " + fields.joinToString(",") { "$it : true" } + " })"
        } else {
            ""
        }}
        ${order.mapToString {
            ".orderBy('${it.name}', '${it.direction}')"
        }}
        """.trimIndent()
    }

    /**
     * 输出统计SQL语句
     */
    fun toCountSql(): String {
        return """
        ${toSql0()}
        .count()
        """.trimIndent()
    }
}

/**
 * 数据库查询条件
 */
class DbCondition(val name: String, val oper: String)

/**
 * 数据库排序
 */
class DbOrder(val name: String, val direction: String = "asc")

/**
 * 页面请求信息，包括搜索参数，排序参数，分页参数，返回前一个页面参数等等
 *
 * 3 个参数均为非空，是为了方便页面，避免各种null判断
 * 为了Spring能够设置参数，需要给每个参数设置默认值
 */
class PageReq(
        var pageParam: PageParam = PageParam(10, 0),
        var searchParams: Collection<SearchParam> = emptyList(),
        var sortParam: SortParam = SortParam("")
) {

    fun <T : CloudBase> list(cs: CloudStatic<T>, conditions: List<DbCondition> = emptyList()): PagedList<T> {
        val stmt = DbStatement(cs)

        searchParams.filter { it.value.isNotBlank() }.forEach {
            it.applyTo(stmt)
        }

        if (conditions.isNotEmpty()) {
            stmt.conditions.addAll(conditions)
        }

        sortParam.toDbOrder()?.let { stmt.order = it }
        pageParam.applyTo(stmt)

        return stmt.query()
    }

    /**
     * 获取上下关联对象：即：前一个，后一个
     */
    fun <T : CloudBase> side(cs: CloudStatic<T>, position: Int): List<T> {
        val stmt = DbStatement(cs)

        searchParams.filter { it.value.isNotBlank() }.forEach {
            it.applyTo(stmt)
        }

        sortParam.toDbOrder()?.let { stmt.order = it }
        pageParam.applyTo(stmt)

        val wxCloudDbService = WxCloudContext.getWxCloudService().dbService()

        var first = position - 1
        var max = 3
        if (position == 0) {
            first = 0
            max = 2
        }

        stmt.skip = first
        stmt.limit = max

        val queryResult = wxCloudDbService.query(WxCloudDbQuery(stmt.toQuerySql()))
        return queryResult.data!!.map { cs.parse(it) }
    }

    val searchString = searchParams.filter { !it.value.isBlank() }.joinToString("&") { it.queryString } + "&" + pageParam.queryString + "&" + sortParam.queryString
    val searchStringNoSort = searchParams.filter { !it.value.isBlank() }.joinToString("&") { "${it.raw}=${it.value}" } + "&" + pageParam.queryString

    companion object {
        fun create(page: PageParam?, search: Array<String>?, sort: SortParam?): PageReq {
            return create(page, search?.mapNotNull { SearchParam.parse(it) }, sort)
        }

        fun create(pageParam: PageParam?, searchParams: Collection<SearchParam>?, sortParam: SortParam?): PageReq {
            return PageReq(pageParam
                    ?: PageParam(10, 0),
                    searchParams ?: emptyList(),
                    sortParam ?: SortParam(""))
        }
    }

}

/**
 * 搜索有关的参数
 * list页面需要增加新的搜索运算符的时候，需要在这里实现。
 * 页面模板示例：
 * <input class="form-control input-sm" type="text" name="search_CONTAINS_name" th:value="${param.search_CONTAINS_name}" />
 */
class SearchParam(val property: String, val oper: String, val value: String, val kind: String = "String") {

    var raw: String = ""

    fun <T : CloudBase> applyTo(stmt: DbStatement<T>): DbStatement<T> {
        val cond =
                when (oper) {
                    "EQ" -> DbCondition(property, """ '$value' """)
                    "CONTAINS" -> DbCondition(property, """ /$value/ """)
                    "GE" -> DbCondition(property, """ _.gte($value) """)
                    "GT" -> DbCondition(property, """ _.gt($value) """)
                    "LE" -> DbCondition(property, """ _.lte($value) """)
                    "LT" -> DbCondition(property, """ _.lt($value) """)
                    else -> null
                }
        cond?.also { stmt.conditions.add(cond) }
        return stmt
    }

    val queryString = "$raw=$value"

    companion object {
        /**
         * 页面通过这个方法，将String转化为SearchParam, 将Array<String>转化为List<SearchParam>
         */
        fun parse(str: String): SearchParam? {
            //兼容search_前缀
            @Suppress("NAME_SHADOWING") var str = str
            if (str.startsWith("search_")) str = str.substringAfter("search_")

            val value = str.substringAfter('=')
            val arr = str.substringBefore('=').split('_')
            return if (arr.size >= 2) {
                val oper = arr[0]
                val property = arr[1]
                var kind = "String"
                if (arr.size >= 3) {
                    kind = arr[2]
                }
                SearchParam(property, oper, value, kind).also { it.raw = str }
            } else {
                null
            }
        }
    }
}

/**
 * 排序有关的参数
 * 为了Spring能够设置参数，需要给每个参数设置默认值
 */
class SortParam(val name: String = "", val direction: String = "asc") {

    val queryString = "page_sort=$name&page_sort_dir=$direction"

    fun toDbOrder(): DbOrder? {
        if (name.isEmpty()) {
            return null
        }
        return DbOrder(name, direction)
    }

    companion object {}

}

/**
 * 分页有关的参数
 */
data class PageParam(val pageSize: Int = 10, val pageNumber: Int = 0) {

    fun <T : CloudBase> applyTo(stmt: DbStatement<T>): DbStatement<T> {
        return stmt.apply(this)
    }

    val queryString = "page_index=${pageNumber + 1}"

    companion object {}
}

class PagedList<T>(
        /* 页码数据 */
        val content: List<T>,

        /* 每页记录数 */
        val size: Int = 0,

        /* 当前页码 */
        val number: Int = 0,

        /* 总共记录数 */
        val totalElements: Int = 0

) : Iterable<T> {

    /**
     * 总共页数
     */
    val totalPages: Int = if (size == 0) 1 else ceil(1.0 * totalElements / size).toInt()

    override fun iterator(): Iterator<T> = content.iterator()

    fun hasPrevious(): Boolean = number > 0

    fun isFirst(): Boolean = !hasPrevious()

    fun hasNext(): Boolean = number + 1 < totalPages

    fun isLast(): Boolean = !hasNext()

    /**
     * 页码显示的页码列表（不含头尾）
     */
    fun numberList(count: Int): List<Int>? {
        var begin = number + 1 - 2
        if (begin < 2) begin = 2
        var end = begin + (count - 1)
        if (end >= totalPages) {
            end = totalPages - 1
        }

        if (end - begin < (count - 1)) {
            begin = end - (count - 1)
        }
        if (begin < 2) begin = 2
        if (end >= begin) {
            return (begin..end).toList()
        }
        return null
    }

    fun <R> mapToPagedList(transform: (T) -> R): PagedList<R> {
        return PagedList(content.map(transform), size, number, totalElements)
    }

}