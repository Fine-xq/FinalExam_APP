package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import java.io.Serializable

/**
 * 聊天消息：买家与卖家之间的单向消息，read 表示接收方是否已读。
 */
data class ChatMessage(
    val id: Int = 0,
    val fromId: Int,
    val fromName: String,
    val toId: Int,
    val toName: String,
    val goodsId: Int = 0,
    val goodsName: String = "",
    val content: String,
    val time: Long = System.currentTimeMillis(),
    val read: Boolean = false
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * 会话摘要（按聊天对象聚合后的结果，不落盘）。
 */
data class Conversation(
    val peerId: Int,
    val peerName: String,
    val goodsId: Int,
    val goodsName: String,
    val lastMessage: String,
    val lastFromMe: Boolean,
    val lastTime: Long,
    val unread: Int
)
