package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

interface MessageDao {
    suspend fun sendMessage(message: ChatMessage): Boolean
    suspend fun getMessagesWith(userId: Int, peerId: Int): List<ChatMessage>
    suspend fun getConversations(userId: Int): List<Conversation>
    suspend fun markConversationRead(userId: Int, peerId: Int)
    suspend fun unreadCount(userId: Int): Int
}
