package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R

/**
 * 聊天记录适配器：自己发的靠右（橙色气泡），对方发的靠左（白色气泡）。
 */
class ChatAdapter : RecyclerView.Adapter<ChatAdapter.Holder>() {

    private val items = ArrayList<ChatMessage>()

    fun submit(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position].fromId == Session.uid) TYPE_MINE else TYPE_OTHER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val layout = if (viewType == TYPE_MINE) R.layout.item_chat_mine else R.layout.item_chat_other
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val content: TextView = itemView.findViewById(R.id.tv_content)
        private val time: TextView = itemView.findViewById(R.id.tv_time)

        fun bind(message: ChatMessage) {
            content.text = message.content
            time.text = formatMessageTime(message.time)
        }
    }

    companion object {
        private const val TYPE_MINE = 1
        private const val TYPE_OTHER = 2
    }
}
