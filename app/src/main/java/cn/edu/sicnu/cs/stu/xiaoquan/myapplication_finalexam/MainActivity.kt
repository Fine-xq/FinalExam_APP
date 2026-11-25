package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 主页：TabLayout + ViewPager2 + Fragment 实现多分区浏览。
 */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Session.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val categories = ArrayList<Category?>()
        categories.add(null)
        categories.addAll(Category.values().toList())

        val pager = findViewById<ViewPager2>(R.id.viewpager)
        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = categories.size
            override fun createFragment(position: Int): Fragment =
                GoodsFragment.newInstance(categories[position])
        }

        TabLayoutMediator(findViewById<TabLayout>(R.id.tabs), pager) { tab, position ->
            tab.text = categories[position]?.let { getString(it.labelRes) }
                ?: getString(R.string.tab_all)
        }.attach()

        findViewById<FloatingActionButton>(R.id.fab_publish).setOnClickListener {
            startActivity(Intent(this, PublishActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_profile -> {
            startActivity(Intent(this, ProfileActivity::class.java))
            true
        }
        R.id.action_my_goods -> {
            startActivity(Intent(this, MyGoodsActivity::class.java))
            true
        }
        R.id.action_orders -> {
            startActivity(Intent(this, OrdersActivity::class.java))
            true
        }
        R.id.action_lang -> {
            switchLanguage()
            true
        }
        R.id.action_about -> {
            showAbout()
            true
        }
        R.id.action_logout -> {
            confirmLogout()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /** 在中文与英文之间切换，保存偏好后立即生效（系统会重建 Activity）。 */
    private fun switchLanguage() {
        val next = if (LocaleManager.getLang(this) == "zh-CN") "en-US" else "zh-CN"
        LocaleManager.setLang(this, next)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next))
        recreate()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.action_logout) { _, _ ->
                Session.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
