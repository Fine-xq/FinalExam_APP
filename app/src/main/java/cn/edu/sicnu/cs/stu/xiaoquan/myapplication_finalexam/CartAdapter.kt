package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.button.MaterialButton

/**
 * 购物车列表适配器：复用商品卡片样式，右侧提供删除按钮。
 */
class CartAdapter(
    private val onDelete: (Goods) -> Unit
) : RecyclerView.Adapter<CartAdapter.Holder>() {

    private val items = ArrayList<Goods>()

    fun submit(list: List<Goods>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverEmoji: TextView = itemView.findViewById(R.id.tv_cover)
        private val coverImage: ImageView = itemView.findViewById(R.id.iv_cover)
        private val name: TextView = itemView.findViewById(R.id.tv_name)
        private val seller: TextView = itemView.findViewById(R.id.tv_seller)
        private val price: TextView = itemView.findViewById(R.id.tv_price)
        private val status: TextView = itemView.findViewById(R.id.tv_status)
        private val delete: MaterialButton = itemView.findViewById(R.id.btn_delete)

        fun bind(goods: Goods) {
            val ctx = itemView.context
            coverEmoji.text = goods.category.emoji
            coverEmoji.visibility = View.VISIBLE
            coverImage.setImageDrawable(null)
            coverImage.visibility = View.GONE
            ImageLoader.loadCover(goods, ctx.filesDir, coverImage) {
                coverEmoji.visibility = View.GONE
            }

            name.text = goods.name
            seller.text = ctx.getString(R.string.seller_label, goods.sellerName)
            price.text = ctx.getString(R.string.price_format, formatPrice(goods.price))
            itemView.alpha = if (goods.isSold) 0.55f else 1f
            if (goods.isSold) {
                status.visibility = View.VISIBLE
                status.text = ctx.getString(R.string.status_sold)
            } else {
                status.visibility = View.GONE
            }
            delete.setOnClickListener { onDelete(goods) }
        }
    }
}
