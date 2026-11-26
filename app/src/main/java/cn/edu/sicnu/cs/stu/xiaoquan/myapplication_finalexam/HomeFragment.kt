package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主页：顶部搜索框 + 分区页签 + 商品列表 + 发布按钮。
 * 输入关键字后隐藏分区页签，直接展示跨分区的搜索结果。
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val categories = ArrayList<Category?>()
    private val searchAdapter = GoodsAdapter(
        isMine = false,
        onItemClick = { openDetail(it) },
        onActionClick = { buy(it) }
    )
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categories.clear()
        categories.add(null)
        categories.addAll(Category.values())

        val pager = view.findViewById<ViewPager2>(R.id.viewpager)
        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = categories.size
            override fun createFragment(position: Int): Fragment =
                GoodsFragment.newInstance(categories[position])
        }
        TabLayoutMediator(view.findViewById<TabLayout>(R.id.tabs), pager) { tab, position ->
            tab.text = categories[position]?.let { getString(it.labelRes) }
                ?: getString(R.string.tab_all)
        }.attach()

        val recycler = view.findViewById<RecyclerView>(R.id.rv_search_result)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = searchAdapter

        view.findViewById<FloatingActionButton>(R.id.fab_publish).setOnClickListener {
            startActivity(Intent(requireContext(), PublishActivity::class.java))
        }

        view.findViewById<EditText>(R.id.et_search).doAfterTextChanged { text ->
            onKeywordChanged(view, text?.toString().orEmpty().trim())
        }
    }

    private fun onKeywordChanged(root: View, keyword: String) {
        val tabs = root.findViewById<TabLayout>(R.id.tabs)
        val pager = root.findViewById<ViewPager2>(R.id.viewpager)
        val recycler = root.findViewById<RecyclerView>(R.id.rv_search_result)
        val empty = root.findViewById<TextView>(R.id.tv_search_empty)

        val searching = keyword.isNotEmpty()
        tabs.visibility = if (searching) View.GONE else View.VISIBLE
        pager.visibility = if (searching) View.GONE else View.VISIBLE

        if (!searching) {
            searchJob?.cancel()
            searchAdapter.submit(emptyList())
            recycler.visibility = View.GONE
            empty.visibility = View.GONE
            return
        }

        searchJob?.cancel()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                Repository.getAllGoods().filter {
                    it.name.contains(keyword, true) || it.desc.contains(keyword, true)
                }
            }
            searchAdapter.submit(list)
            val none = list.isEmpty()
            recycler.visibility = if (none) View.GONE else View.VISIBLE
            empty.visibility = if (none) View.VISIBLE else View.GONE
        }
    }

    private fun openDetail(goods: Goods) {
        startActivity(
            Intent(requireContext(), DetailActivity::class.java)
                .putExtra(DetailActivity.EXTRA_GOODS_ID, goods.id)
        )
    }

    private fun buy(goods: Goods) {
        ShopActions.confirmBuy(requireContext(), goods, viewLifecycleOwner.lifecycleScope) {
            val root = view ?: return@confirmBuy
            val input = root.findViewById<EditText>(R.id.et_search)
            onKeywordChanged(root, input.text?.toString().orEmpty().trim())
        }
    }
}
