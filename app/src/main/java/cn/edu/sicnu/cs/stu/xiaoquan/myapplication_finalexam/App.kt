package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        applySavedLocale()
        Repository.init(this)
    }

    /** 读取用户保存的语言，默认中文（对应作业一：多语言）。 */
    private fun applySavedLocale() {
        val lang = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("lang", "zh-CN") ?: "zh-CN"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
    }
}
