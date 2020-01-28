package cn.hillwind.wx.cloud


/**
 * 上一条、下一条
 *
 * @param <T>
 */
class RowSide<T : CloudBase>(val prev: T?, val self: T, val next: T?) {

    // 下面3个属性是方便页面用的，避免：${side.prev.id} 写法可能带来的null问题


    val selfId = self.id
    val nextId = next?.id
    val prevId = prev?.id

    companion object {

        fun <T : CloudBase> create(list: List<T>, beanId: String): RowSide<T>? {
            if (list.isEmpty()) {
                return null
            }
            val len = list.size
            if (len == 3 && list[1].id == beanId) {
                return RowSide(list[0], list[1], list[2])
            } else if (len == 2) {
                if (list[0].id == beanId) {
                    return RowSide(null, list[0], list[1])
                } else if (list[1].id == beanId) {
                    return RowSide(list[0], list[1], null)
                }
            } else if (len == 1 && list[0].id == beanId) {
                return RowSide(null, list[0], null)
            }
            return null
        }
    }
}
