package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import java.io.Serializable

/**
 * 购物车条目：只记录商品与所属用户，商品信息从商品表实时读取。
 */
data class CartItem(
    val goodsId: Int,
    val buyerId: Int,
    val addedAt: Long = System.currentTimeMillis()
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
