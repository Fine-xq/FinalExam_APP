package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrdersActivity : BaseActivity() {

    private val adapter = OrderAdapter()
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private var showingSold = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        recycler = findViewById(R.id.rv_orders)
        emptyView = findViewById(R.id.tv_empty)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<MaterialButtonToggleGroup>(R.id.toggle_orders)
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                showingSold = checkedId == R.id.btn_sold
                loadOrders()
            }

        loadOrders()
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                if (showingSold) Repository.getOrdersSold(Session.uid)
                else Repository.getOrdersBought(Session.uid)
            }
            adapter.submit(data, showingSold)
            val isEmpty = data.isEmpty()
            emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }
}
