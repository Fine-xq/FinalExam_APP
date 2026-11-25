package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val items = ArrayList<Order>()
    private var soldView = false
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    fun submit(list: List<Order>, sold: Boolean) {
        items.clear()
        items.addAll(list)
        soldView = sold
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tv_goods_name)
        private val role: TextView = itemView.findViewById(R.id.tv_role)
        private val info: TextView = itemView.findViewById(R.id.tv_info)
        private val price: TextView = itemView.findViewById(R.id.tv_price)
        private val time: TextView = itemView.findViewById(R.id.tv_time)

        fun bind(order: Order) {
            val ctx = itemView.context
            name.text = order.goodsName
            role.text = ctx.getString(if (soldView) R.string.order_sold else R.string.order_bought)
            info.text = ctx.getString(R.string.order_info, order.buyerName, order.sellerName)
            price.text = ctx.getString(R.string.price_format, formatPrice(order.price))
            time.text = dateFormat.format(Date(order.time))
        }
    }
}
