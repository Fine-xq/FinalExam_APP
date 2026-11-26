package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 聊天页：买家与卖家一对一沟通。进入即标记已读，发送后重新加载并滚到底部。
 */
class ChatActivity : BaseActivity() {

    private var peerId = 0
    private var peerName = ""
    private var goodsId = 0
    private var goodsName = ""

    private val adapter = ChatAdapter()
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var etMessage: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        peerId = intent.getIntExtra(EXTRA_PEER_ID, 0)
        peerName = intent.getStringExtra(EXTRA_PEER_NAME).orEmpty()
        goodsId = intent.getIntExtra(EXTRA_GOODS_ID, 0)
        goodsName = intent.getStringExtra(EXTRA_GOODS_NAME).orEmpty()
        if (peerId == 0) {
            finish()
            return
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = peerName.ifBlank { getString(R.string.nav_messages) }
        toolbar.setNavigationOnClickListener { finish() }

        val banner = findViewById<TextView>(R.id.tv_goods_banner)
        if (goodsName.isNotBlank()) {
            banner.visibility = View.VISIBLE
            banner.text = getString(R.string.chat_goods_prefix, goodsName)
        }

        recycler = findViewById(R.id.rv_chat)
        emptyView = findViewById(R.id.tv_chat_empty)
        etMessage = findViewById(R.id.et_message)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<MaterialButton>(R.id.btn_send).setOnClickListener { send() }
        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send()
                true
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadMessages()
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                Repository.markConversationRead(Session.uid, peerId)
                Repository.getMessagesWith(Session.uid, peerId)
            }
            adapter.submit(list)
            val none = list.isEmpty()
            emptyView.visibility = if (none) View.VISIBLE else View.GONE
            recycler.visibility = if (none) View.GONE else View.VISIBLE
            if (list.isNotEmpty()) recycler.scrollToPosition(list.size - 1)
        }
    }

    private fun send() {
        val text = etMessage.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        etMessage.setText("")
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                Repository.sendMessage(
                    ChatMessage(
                        fromId = Session.uid,
                        fromName = Session.username,
                        toId = peerId,
                        toName = peerName,
                        goodsId = goodsId,
                        goodsName = goodsName,
                        content = text
                    )
                )
            }
            if (ok) {
                loadMessages()
            } else {
                Toast.makeText(this@ChatActivity, R.string.msg_send_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_PEER_ID = "peerId"
        const val EXTRA_PEER_NAME = "peerName"
        const val EXTRA_GOODS_ID = "goodsId"
        const val EXTRA_GOODS_NAME = "goodsName"
    }
}
