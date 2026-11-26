package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R

/**
 * 消息页的会话列表适配器：按聊天对象聚合，显示最后一条消息与未读数。
 */
class ConversationAdapter(
    private val onClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.Holder>() {

    private val items = ArrayList<Conversation>()

    fun submit(list: List<Conversation>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: TextView = itemView.findViewById(R.id.tv_avatar)
        private val peer: TextView = itemView.findViewById(R.id.tv_peer)
        private val goods: TextView = itemView.findViewById(R.id.tv_goods)
        private val time: TextView = itemView.findViewById(R.id.tv_time)
        private val last: TextView = itemView.findViewById(R.id.tv_last)
        private val unread: TextView = itemView.findViewById(R.id.tv_unread)

        fun bind(conversation: Conversation) {
            val ctx = itemView.context
            avatar.text = conversation.peerName.take(1).uppercase().ifEmpty { "?" }
            peer.text = conversation.peerName
            if (conversation.goodsName.isNotBlank()) {
                goods.visibility = View.VISIBLE
                goods.text = ctx.getString(R.string.msg_goods_prefix, conversation.goodsName)
            } else {
                goods.visibility = View.GONE
            }
            time.text = formatMessageTime(conversation.lastTime)
            last.text = if (conversation.lastFromMe) {
                ctx.getString(R.string.msg_you, conversation.lastMessage)
            } else {
                conversation.lastMessage
            }
            if (conversation.unread > 0) {
                unread.visibility = View.VISIBLE
                unread.text = if (conversation.unread > 99) "99+" else conversation.unread.toString()
            } else {
                unread.visibility = View.GONE
            }
            itemView.setOnClickListener { onClick(conversation) }
        }
    }
}
