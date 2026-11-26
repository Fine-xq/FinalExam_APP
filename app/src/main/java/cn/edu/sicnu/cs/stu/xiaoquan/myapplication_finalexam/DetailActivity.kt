package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : BaseActivity() {

    private var goods: Goods? = null

    private lateinit var coverEmoji: TextView
    private lateinit var coverImage: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvMeta: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDesc: TextView
    private lateinit var btnBuy: MaterialButton
    private lateinit var btnCart: MaterialButton
    private lateinit var btnChat: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        coverEmoji = findViewById(R.id.tv_cover)
        coverImage = findViewById(R.id.iv_cover)
        tvName = findViewById(R.id.tv_name)
        tvPrice = findViewById(R.id.tv_price)
        tvMeta = findViewById(R.id.tv_meta)
        tvStatus = findViewById(R.id.tv_status)
        tvDesc = findViewById(R.id.tv_desc)
        btnBuy = findViewById(R.id.btn_buy)
        btnCart = findViewById(R.id.btn_cart)
        btnChat = findViewById(R.id.btn_chat)

        val goodsId = intent.getIntExtra(EXTRA_GOODS_ID, 0)
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) { Repository.getGoodsById(goodsId) }
            if (data == null) {
                finish()
                return@launch
            }
            goods = data
            bind(data)
        }
    }

    private fun bind(data: Goods) {
        coverEmoji.text = data.category.emoji
        coverEmoji.visibility = View.VISIBLE
        coverImage.setImageDrawable(null)
        coverImage.visibility = View.GONE
        ImageLoader.loadCover(data, filesDir, coverImage) {
            coverEmoji.visibility = View.GONE
        }

        tvName.text = data.name
        tvPrice.text = getString(R.string.price_format, formatPrice(data.price))
        tvMeta.text = getString(
            R.string.detail_meta,
            getString(R.string.category_label, getString(data.category.labelRes)),
            getString(R.string.seller_label, data.sellerName)
        )
        tvStatus.text = getString(if (data.isSold) R.string.status_sold else R.string.status_on_sale)
        tvDesc.text = data.desc

        val own = data.sellerId == Session.uid
        val editable = own && !data.isSold
        btnBuy.isEnabled = editable || (!data.isSold && !own)
        btnBuy.text = when {
            editable -> getString(R.string.action_edit)
            data.isSold -> getString(R.string.status_sold)
            own -> getString(R.string.own_goods_hint)
            else -> getString(R.string.action_buy)
        }
        btnBuy.setOnClickListener {
            val current = goods ?: return@setOnClickListener
            if (editable) {
                startActivity(
                    Intent(this, PublishActivity::class.java)
                        .putExtra(PublishActivity.EXTRA_GOODS_ID, current.id)
                )
            } else {
                ShopActions.confirmBuy(this, current, lifecycleScope) { reload(current.id) }
            }
        }

        // 自己的商品不显示「联系卖家 / 加入购物车」
        btnChat.visibility = if (own) View.GONE else View.VISIBLE
        btnCart.visibility = if (own) View.GONE else View.VISIBLE
        btnCart.isEnabled = !data.isSold
        btnChat.setOnClickListener {
            startActivity(
                Intent(this, ChatActivity::class.java)
                    .putExtra(ChatActivity.EXTRA_PEER_ID, data.sellerId)
                    .putExtra(ChatActivity.EXTRA_PEER_NAME, data.sellerName)
                    .putExtra(ChatActivity.EXTRA_GOODS_ID, data.id)
                    .putExtra(ChatActivity.EXTRA_GOODS_NAME, data.name)
            )
        }
        btnCart.setOnClickListener { addToCart(data) }
    }

    private fun addToCart(data: Goods) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                when {
                    Repository.getCartItems(Session.uid).any { it.goodsId == data.id } ->
                        R.string.add_cart_exists
                    Repository.addToCart(data.id, Session.uid) -> R.string.add_cart_success
                    else -> R.string.add_cart_failed
                }
            }
            Toast.makeText(this@DetailActivity, result, Toast.LENGTH_SHORT).show()
        }
    }

    /** 从编辑页返回时刷新详情。 */
    override fun onResume() {
        super.onResume()
        goods?.let { reload(it.id) }
    }

    private fun reload(goodsId: Int) {
        lifecycleScope.launch {
            val updated = withContext(Dispatchers.IO) { Repository.getGoodsById(goodsId) }
            if (updated != null) {
                goods = updated
                bind(updated)
            }
        }
    }

    companion object {
        const val EXTRA_GOODS_ID = "goodsId"
    }
}
