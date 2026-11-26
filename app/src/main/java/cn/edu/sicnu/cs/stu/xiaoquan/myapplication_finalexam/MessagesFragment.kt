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
 * 消息页：展示当前用户的全部会话，点击进入聊天页。
 */
class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private val adapter = ConversationAdapter { conversation ->
        startActivity(
            Intent(requireContext(), ChatActivity::class.java)
                .putExtra(ChatActivity.EXTRA_PEER_ID, conversation.peerId)
                .putExtra(ChatActivity.EXTRA_PEER_NAME, conversation.peerName)
                .putExtra(ChatActivity.EXTRA_GOODS_ID, conversation.goodsId)
                .putExtra(ChatActivity.EXTRA_GOODS_NAME, conversation.goodsName)
        )
    }

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.findViewById(R.id.rv_conversations)
        emptyView = view.findViewById(R.id.tv_empty)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        if (Session.isLoggedIn && view != null) loadConversations()
    }

    private fun loadConversations() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) { Repository.getConversations(Session.uid) }
            adapter.submit(list)
            val none = list.isEmpty()
            emptyView.visibility = if (none) View.VISIBLE else View.GONE
            recycler.visibility = if (none) View.GONE else View.VISIBLE
        }
    }
}
