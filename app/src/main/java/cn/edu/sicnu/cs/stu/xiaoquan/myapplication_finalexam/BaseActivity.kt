package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

/**
 * 所有 Activity 的基类：在创建时按保存的语言包装 Context，使界面立即显示对应语言。
 */
open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }
}
