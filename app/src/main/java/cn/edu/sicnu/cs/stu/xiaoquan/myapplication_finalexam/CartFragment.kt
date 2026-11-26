package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 购物车页：展示已加入的商品，支持移除与一键结算（结算即逐个下单）。
 */
class CartFragment : Fragment(R.layout.fragment_cart) {

    private val adapter = CartAdapter { goods -> removeFromCart(goods) }
    private var cartGoods: List<Goods> = emptyList()

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnCheckout: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.findViewById(R.id.rv_cart)
        emptyView = view.findViewById(R.id.tv_empty)
        tvTotal = view.findViewById(R.id.tv_total)
        btnCheckout = view.findViewById(R.id.btn_checkout)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
        btnCheckout.setOnClickListener { confirmCheckout() }
    }

    override fun onResume() {
        super.onResume()
        if (Session.isLoggedIn && view != null) loadCart()
    }

    private fun loadCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) { Repository.getCartGoods(Session.uid) }
            cartGoods = data
            adapter.submit(data)
            val payable = data.filter { !it.isSold }
            tvTotal.text = getString(R.string.cart_total, formatPrice(payable.sumOf { it.price }))
            btnCheckout.isEnabled = payable.isNotEmpty()
            val none = data.isEmpty()
            emptyView.visibility = if (none) View.VISIBLE else View.GONE
            recycler.visibility = if (none) View.GONE else View.VISIBLE
        }
    }

    private fun removeFromCart(goods: Goods) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                Repository.removeFromCart(goods.id, Session.uid)
            }
            Toast.makeText(requireContext(), R.string.cart_removed, Toast.LENGTH_SHORT).show()
            loadCart()
        }
    }

    private fun confirmCheckout() {
        val payable = cartGoods.filter { !it.isSold }
        if (payable.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.checkout_confirm_title)
            .setMessage(
                getString(
                    R.string.checkout_confirm_message,
                    payable.size,
                    formatPrice(payable.sumOf { it.price })
                )
            )
            .setPositiveButton(R.string.action_checkout) { _, _ -> checkout(payable) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun checkout(payable: List<Goods>) {
        viewLifecycleOwner.lifecycleScope.launch {
            var success = 0
            var failed = 0
            for (goods in payable) {
                val ok = withContext(Dispatchers.IO) {
                    val bought = Repository.buyGoods(goods.id, Session.uid, Session.username)
                    if (bought) Repository.removeFromCart(goods.id, Session.uid)
                    bought
                }
                if (ok) success++ else failed++
            }
            val ctx = context ?: return@launch
            if (success > 0) {
                Toast.makeText(
                    ctx,
                    getString(R.string.checkout_success, success),
                    Toast.LENGTH_SHORT
                ).show()
                runCatching { ctx.startService(Intent(ctx, SoundService::class.java)) }
            }
            if (failed > 0) {
                Toast.makeText(ctx, getString(R.string.checkout_failed, failed), Toast.LENGTH_SHORT)
                    .show()
            }
            loadCart()
        }
    }
}
