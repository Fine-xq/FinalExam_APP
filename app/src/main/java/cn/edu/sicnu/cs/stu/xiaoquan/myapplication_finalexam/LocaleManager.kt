package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * 应用内语言管理：默认中文，偏好保存在 SharedPreferences。
 * 通过 Context.createConfigurationContext 在 Activity 创建时注入语言，
 * 保证首次启动即生效（对应作业一：多语言）。
 */
object LocaleManager {

    private const val PREFS = "settings"
    private const val KEY_LANG = "lang"
    private const val DEFAULT_LANG = "zh-CN"

    fun getLang(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, DEFAULT_LANG) ?: DEFAULT_LANG

    fun setLang(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, lang).apply()
    }

    fun wrap(context: Context): Context {
        val locale = Locale.forLanguageTag(getLang(context))
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
