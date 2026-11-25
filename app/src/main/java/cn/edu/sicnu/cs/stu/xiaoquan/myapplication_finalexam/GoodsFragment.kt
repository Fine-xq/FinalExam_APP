package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 单个商品分区的列表页（对应作业六：多 Fragment 应用）。
 */
class GoodsFragment : Fragment(R.layout.fragment_goods) {

    private val adapter = GoodsAdapter(
        isMine = false,
        onItemClick = { goods -> openDetail(goods) },
        onActionClick = { goods -> buy(goods) }
    )

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private var category: Category? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString(ARG_CATEGORY)?.let { Category.valueOf(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.findViewById(R.id.rv_goods)
        emptyView = view.findViewById(R.id.tv_empty)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        if (Session.isLoggedIn && view != null) {
            loadGoods()
        }
    }

    private fun loadGoods() {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                val all = Repository.getAllGoods()
                val filter = category
                if (filter == null) all else all.filter { it.category == filter }
            }
            adapter.submit(data)
            val isEmpty = data.isEmpty()
            emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
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
            loadGoods()
        }
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: Category?): GoodsFragment {
            val fragment = GoodsFragment()
            fragment.arguments = Bundle().apply {
                if (category != null) putString(ARG_CATEGORY, category.name)
            }
            return fragment
        }
    }
}
