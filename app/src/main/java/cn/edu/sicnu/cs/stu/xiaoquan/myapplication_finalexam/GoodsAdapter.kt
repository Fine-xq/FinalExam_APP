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
 * RecyclerView 商品适配器（对应作业五：RecyclerView 与适配器）。
 */
class GoodsAdapter(
    private val isMine: Boolean,
    private val onItemClick: (Goods) -> Unit,
    private val onActionClick: (Goods) -> Unit,
    private val onEditClick: ((Goods) -> Unit)? = null
) : RecyclerView.Adapter<GoodsAdapter.GoodsViewHolder>() {

    private val items = ArrayList<Goods>()

    fun submit(list: List<Goods>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoodsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goods, parent, false)
        return GoodsViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: GoodsViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class GoodsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverEmoji: TextView = itemView.findViewById(R.id.tv_cover)
        private val coverImage: ImageView = itemView.findViewById(R.id.iv_cover)
        private val name: TextView = itemView.findViewById(R.id.tv_name)
        private val desc: TextView = itemView.findViewById(R.id.tv_desc)
        private val price: TextView = itemView.findViewById(R.id.tv_price)
        private val category: TextView = itemView.findViewById(R.id.tv_category)
        private val seller: TextView = itemView.findViewById(R.id.tv_seller)
        private val action: MaterialButton = itemView.findViewById(R.id.btn_action)
        private val edit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        private val status: TextView = itemView.findViewById(R.id.tv_status)

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
            desc.text = goods.desc
            price.text = ctx.getString(R.string.price_format, formatPrice(goods.price))
            category.text = "${goods.category.emoji} ${ctx.getString(goods.category.labelRes)}"
            seller.text = ctx.getString(R.string.seller_label, goods.sellerName)
            status.text = ctx.getString(if (goods.isSold) R.string.status_sold else R.string.status_on_sale)
            status.visibility = View.VISIBLE
            itemView.alpha = if (goods.isSold) 0.55f else 1f

            if (isMine) {
                action.text = ctx.getString(R.string.action_delete)
                action.isEnabled = !goods.isSold
                edit.visibility = View.VISIBLE
                edit.isEnabled = !goods.isSold
                edit.setOnClickListener { onEditClick?.invoke(goods) }
            } else {
                edit.visibility = View.GONE
                val own = goods.sellerId == Session.uid
                action.text = when {
                    goods.isSold -> ctx.getString(R.string.status_sold)
                    own -> ctx.getString(R.string.own_goods_hint)
                    else -> ctx.getString(R.string.action_buy)
                }
                action.isEnabled = !goods.isSold && !own
            }

            action.setOnClickListener { onActionClick(goods) }
            itemView.setOnClickListener { onItemClick(goods) }
        }
    }
}
