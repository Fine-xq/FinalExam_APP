package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 应用主框架：顶部标题 + 四个底部页签（主页 / 消息 / 购物车 / 我的）。
 * 四个 Fragment 用 add + hide/show 复用，避免重复创建与重复加载。
 */
class MainActivity : BaseActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView
    private var currentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Session.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        bottomNav = findViewById(R.id.bottom_nav)

        // 先恢复页签选中项，再注册监听，避免重建时切到错误页面
        bottomNav.selectedItemId = savedInstanceState?.getInt(KEY_TAB) ?: R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }
        showTab(bottomNav.selectedItemId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB, bottomNav.selectedItemId)
    }

    override fun onResume() {
        super.onResume()
        refreshMessageBadge()
    }

    /** 切换到指定页签。每次用新的 Fragment 替换，避免 ViewPager2 被 hide 后不刷新。 */
    private fun showTab(itemId: Int) {
        val tag = when (itemId) {
            R.id.nav_messages -> TAG_MESSAGES
            R.id.nav_cart -> TAG_CART
            R.id.nav_profile -> TAG_PROFILE
            else -> TAG_HOME
        }
        toolbar.setTitle(
            when (itemId) {
                R.id.nav_messages -> R.string.nav_messages
                R.id.nav_cart -> R.string.nav_cart
                R.id.nav_profile -> R.string.nav_profile
                else -> R.string.app_name
            }
        )
        if (tag == currentTag) return
        currentTag = tag
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, createFragment(tag), tag)
            .commit()
    }

    private fun createFragment(tag: String): Fragment = when (tag) {
        TAG_MESSAGES -> MessagesFragment()
        TAG_CART -> CartFragment()
        TAG_PROFILE -> ProfileFragment()
        else -> HomeFragment()
    }

    /** 消息页签的未读红点。 */
    private fun refreshMessageBadge() {
        if (!Session.isLoggedIn) return
        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) { Repository.unreadCount(Session.uid) }
            val badge = bottomNav.getOrCreateBadge(R.id.nav_messages)
            badge.isVisible = count > 0
            badge.number = count
        }
    }

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_MESSAGES = "messages"
        private const val TAG_CART = "cart"
        private const val TAG_PROFILE = "profile"
        private const val KEY_TAB = "selectedTab"
    }
}
