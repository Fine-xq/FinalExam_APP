package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyGoodsActivity : BaseActivity() {

    private val adapter = GoodsAdapter(
        isMine = true,
        onItemClick = { goods ->
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_GOODS_ID, goods.id)
            )
        },
        onActionClick = { goods -> confirmDelete(goods) },
        onEditClick = { goods -> openEditor(goods) }
    )
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_goods)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        recycler = findViewById(R.id.rv_my_goods)
        emptyView = findViewById(R.id.tv_empty)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadGoods()
    }

    private fun loadGoods() {
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) { Repository.getMyGoods(Session.uid) }
            adapter.submit(data)
            val isEmpty = data.isEmpty()
            emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun openEditor(goods: Goods) {
        startActivity(
            Intent(this, PublishActivity::class.java)
                .putExtra(PublishActivity.EXTRA_GOODS_ID, goods.id)
        )
    }

    private fun confirmDelete(goods: Goods) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(getString(R.string.delete_confirm_message, goods.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        Repository.deleteGoods(goods.id, Session.uid)
                    }
                    Toast.makeText(
                        this@MyGoodsActivity,
                        if (ok) R.string.delete_success else R.string.delete_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                    loadGoods()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
