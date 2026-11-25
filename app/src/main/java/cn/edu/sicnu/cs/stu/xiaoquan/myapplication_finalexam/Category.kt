package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import androidx.annotation.StringRes
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import java.io.Serializable

/**
 * 商品分区（家电、水果等）。标签走 string 资源，支持多语言。
 */
enum class Category(@StringRes val labelRes: Int, val emoji: String) : Serializable {
    APPLIANCE(R.string.cat_appliance, "🔌"),
    FRUIT(R.string.cat_fruit, "🍎"),
    DIGITAL(R.string.cat_digital, "📱"),
    CLOTHING(R.string.cat_clothing, "👕"),
    BOOK(R.string.cat_book, "📚"),
    OTHER(R.string.cat_other, "📦");
}
