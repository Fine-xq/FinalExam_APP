package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import java.io.Serializable
import java.util.Locale

/**
 * 商品实体（对应 MVC 中的 Model）。
 * 实现 Serializable，便于通过文件对象序列化做持久化。
 */
data class Goods(
    val id: Int,
    val name: String,
    val price: Double,
    val desc: String,
    val category: Category,
    val sellerId: Int,
    val sellerName: String,
    val isSold: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** 用户上传的本地封面图，相对 filesDir 的路径；为空则使用网络封面。 */
    val coverLocalPath: String? = null
) : Serializable {
    /** 商品封面：来自网络的图片地址，加载失败时界面回退到分区的表情图标。 */
    val coverUrl: String get() = "https://picsum.photos/seed/flea$id/360/360"

    companion object {
        private const val serialVersionUID = 1L
    }
}

fun formatPrice(price: Double): String = String.format(Locale.CHINA, "%.2f", price)
