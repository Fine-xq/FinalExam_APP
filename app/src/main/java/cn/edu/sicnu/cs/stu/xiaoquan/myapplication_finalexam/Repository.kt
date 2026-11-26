package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Context
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * 本地文件仓库（对应 MVC 的 Model 层 / DAO 实现）。
 * 使用对象序列化 ObjectOutputStream 将用户、商品、订单写入应用私有目录，
 * 对应作业六中的“对象序列化和文件存储”。
 */
object Repository : UserDao, GoodsDao, OrderDao, CartDao, MessageDao {

    private val lock = Any()
    private lateinit var dir: File
    private val users = ArrayList<User>()
    private val goods = ArrayList<Goods>()
    private val orders = ArrayList<Order>()
    private val carts = ArrayList<CartItem>()
    private val messages = ArrayList<ChatMessage>()
    private var initialized = false

    private val usersFile: File get() = File(dir, "users.dat")
    private val goodsFile: File get() = File(dir, "goods.dat")
    private val ordersFile: File get() = File(dir, "orders.dat")
    private val cartsFile: File get() = File(dir, "carts.dat")
    private val messagesFile: File get() = File(dir, "messages.dat")

    fun init(context: Context) {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            dir = File(context.applicationContext.filesDir, "shop").apply { mkdirs() }
            users.addAll(readList(usersFile))
            goods.addAll(readList(goodsFile))
            orders.addAll(readList(ordersFile))
            carts.addAll(readList(cartsFile))
            messages.addAll(readList(messagesFile))
            seedIfEmpty()
            initialized = true
        }
    }

    // ---------------- 用户 ----------------

    override suspend fun register(user: User): Boolean = synchronized(lock) {
        if (users.any { it.username == user.username }) return false
        val newId = (users.maxOfOrNull { it.id } ?: 0) + 1
        users.add(user.copy(id = newId))
        persistUsers()
        true
    }

    override suspend fun login(username: String, password: String): User? = synchronized(lock) {
        users.find { it.username == username && it.password == password }
    }

    override suspend fun findUserByName(username: String): User? = synchronized(lock) {
        users.find { it.username == username }
    }

    override suspend fun getUserById(userId: Int): User? = synchronized(lock) {
        users.find { it.id == userId }
    }

    override suspend fun updateProfile(
        userId: Int,
        nickname: String,
        phone: String,
        address: String
    ): Boolean = synchronized(lock) {
        val index = users.indexOfFirst { it.id == userId }
        if (index < 0) return false
        val old = users[index]
        users[index] = old.copy(
            nickname = nickname.trim(),
            phone = phone.trim(),
            address = address.trim()
        )
        persistUsers()
        true
    }

    override suspend fun updateCredentials(
        userId: Int,
        newUsername: String,
        oldPassword: String,
        newPassword: String
    ): CredentialResult = synchronized(lock) {
        val index = users.indexOfFirst { it.id == userId }
        if (index < 0) return CredentialResult.INVALID
        val old = users[index]
        if (old.password != oldPassword) return CredentialResult.WRONG_PASSWORD

        val name = newUsername.trim()
        val pwd = newPassword.trim()
        if (name.isEmpty()) return CredentialResult.INVALID
        if (pwd.isNotEmpty() && pwd.length < 3) return CredentialResult.INVALID
        if (users.any { it.id != userId && it.username == name }) {
            return CredentialResult.USERNAME_TAKEN
        }

        users[index] = old.copy(
            username = name,
            password = pwd.ifEmpty { old.password }
        )
        persistUsers()
        CredentialResult.SUCCESS
    }

    // ---------------- 商品 ----------------

    override suspend fun publishGoods(goods: Goods): Boolean = synchronized(lock) {
        val newId = (this.goods.maxOfOrNull { it.id } ?: 1000) + 1
        this.goods.add(goods.copy(id = newId))
        persistGoods()
        true
    }

    override suspend fun updateGoods(goods: Goods, sellerId: Int): Boolean = synchronized(lock) {
        val index = this.goods.indexOfFirst { it.id == goods.id && it.sellerId == sellerId }
        if (index < 0) return false
        val old = this.goods[index]
        if (old.isSold) return false
        this.goods[index] = old.copy(
            name = goods.name,
            price = goods.price,
            desc = goods.desc,
            category = goods.category,
            coverLocalPath = goods.coverLocalPath
        )
        persistGoods()
        true
    }

    override suspend fun deleteGoods(goodsId: Int, sellerId: Int): Boolean = synchronized(lock) {
        val target = goods.find { it.id == goodsId && it.sellerId == sellerId } ?: return false
        if (target.isSold) return false
        goods.remove(target)
        persistGoods()
        // 同步清理本地封面图
        target.coverLocalPath?.takeIf { it.isNotEmpty() }?.let { rel ->
            val file = File(dir.parentFile, rel)
            if (file.exists()) file.delete()
        }
        true
    }

    override suspend fun getAllGoods(): List<Goods> = synchronized(lock) {
        goods.sortedByDescending { it.createdAt }.toList()
    }

    override suspend fun getMyGoods(sellerId: Int): List<Goods> = synchronized(lock) {
        goods.filter { it.sellerId == sellerId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getGoodsById(goodsId: Int): Goods? = synchronized(lock) {
        goods.find { it.id == goodsId }
    }

    override suspend fun buyGoods(goodsId: Int, buyerId: Int, buyerName: String): Boolean =
        synchronized(lock) {
            val index = goods.indexOfFirst { it.id == goodsId }
            if (index < 0) return false
            val target = goods[index]
            if (target.isSold || target.sellerId == buyerId) return false
            goods[index] = target.copy(isSold = true)
            orders.add(
                Order(
                    id = (orders.maxOfOrNull { it.id } ?: 2000) + 1,
                    goodsId = target.id,
                    goodsName = target.name,
                    price = target.price,
                    buyerId = buyerId,
                    buyerName = buyerName,
                    sellerId = target.sellerId,
                    sellerName = target.sellerName,
                    time = System.currentTimeMillis()
                )
            )
            // 商品一旦售出，其他用户购物车里的同款也一并清掉
            val removed = carts.removeAll { it.goodsId == goodsId }
            persistGoods()
            persistOrders()
            if (removed) persistCarts()
            true
        }

    // ---------------- 购物车 ----------------

    override suspend fun addToCart(goodsId: Int, buyerId: Int): Boolean = synchronized(lock) {
        val target = goods.find { it.id == goodsId } ?: return false
        if (target.isSold || target.sellerId == buyerId) return false
        if (carts.any { it.goodsId == goodsId && it.buyerId == buyerId }) return false
        carts.add(CartItem(goodsId, buyerId))
        persistCarts()
        true
    }

    override suspend fun getCartItems(buyerId: Int): List<CartItem> = synchronized(lock) {
        carts.filter { it.buyerId == buyerId }.sortedByDescending { it.addedAt }
    }

    override suspend fun getCartGoods(buyerId: Int): List<Goods> = synchronized(lock) {
        carts.filter { it.buyerId == buyerId }
            .sortedByDescending { it.addedAt }
            .mapNotNull { item -> goods.find { it.id == item.goodsId } }
    }

    override suspend fun removeFromCart(goodsId: Int, buyerId: Int) = synchronized(lock) {
        if (carts.removeAll { it.goodsId == goodsId && it.buyerId == buyerId }) persistCarts()
    }

    override suspend fun cartCount(buyerId: Int): Int = synchronized(lock) {
        carts.count { it.buyerId == buyerId }
    }

    // ---------------- 消息 ----------------

    override suspend fun sendMessage(message: ChatMessage): Boolean = synchronized(lock) {
        if (message.content.isBlank() || message.toId == message.fromId) return false
        val newId = (messages.maxOfOrNull { it.id } ?: 3000) + 1
        messages.add(
            message.copy(
                id = newId,
                content = message.content.trim(),
                time = System.currentTimeMillis(),
                read = false
            )
        )
        persistMessages()
        true
    }

    override suspend fun getMessagesWith(userId: Int, peerId: Int): List<ChatMessage> =
        synchronized(lock) {
            messages.filter {
                (it.fromId == userId && it.toId == peerId) || (it.fromId == peerId && it.toId == userId)
            }.sortedBy { it.time }
        }

    override suspend fun getConversations(userId: Int): List<Conversation> = synchronized(lock) {
        messages.filter { it.fromId == userId || it.toId == userId }
            .groupBy { if (it.fromId == userId) it.toId else it.fromId }
            .mapNotNull { (peerId, list) ->
                val last = list.maxByOrNull { it.time } ?: return@mapNotNull null
                Conversation(
                    peerId = peerId,
                    peerName = if (last.fromId == userId) last.toName else last.fromName,
                    goodsId = last.goodsId,
                    goodsName = last.goodsName,
                    lastMessage = last.content,
                    lastFromMe = last.fromId == userId,
                    lastTime = last.time,
                    unread = list.count { it.toId == userId && !it.read }
                )
            }
            .sortedByDescending { it.lastTime }
    }

    override suspend fun markConversationRead(userId: Int, peerId: Int) = synchronized(lock) {
        var changed = false
        messages.replaceAll { msg ->
            if (msg.toId == userId && msg.fromId == peerId && !msg.read) {
                changed = true
                msg.copy(read = true)
            } else {
                msg
            }
        }
        if (changed) persistMessages()
    }

    override suspend fun unreadCount(userId: Int): Int = synchronized(lock) {
        messages.count { it.toId == userId && !it.read }
    }

    // ---------------- 订单 ----------------

    override suspend fun getOrdersBought(userId: Int): List<Order> = synchronized(lock) {
        orders.filter { it.buyerId == userId }.sortedByDescending { it.time }
    }

    override suspend fun getOrdersSold(userId: Int): List<Order> = synchronized(lock) {
        orders.filter { it.sellerId == userId }.sortedByDescending { it.time }
    }

    // ---------------- 文件读写 ----------------

    @Suppress("UNCHECKED_CAST")
    private fun <T> readList(file: File): List<T> {
        if (!file.exists()) return emptyList()
        return try {
            ObjectInputStream(file.inputStream()).use { stream ->
                (stream.readObject() as? List<T>) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun write(file: File, data: Any) {
        try {
            ObjectOutputStream(file.outputStream()).use { it.writeObject(data) }
        } catch (_: Exception) {
            // 演示项目：写入失败不影响内存数据
        }
    }

    private fun persistUsers() = write(usersFile, ArrayList(users))
    private fun persistGoods() = write(goodsFile, ArrayList(goods))
    private fun persistOrders() = write(ordersFile, ArrayList(orders))
    private fun persistCarts() = write(cartsFile, ArrayList(carts))
    private fun persistMessages() = write(messagesFile, ArrayList(messages))

    private fun seedIfEmpty() {
        val now = System.currentTimeMillis()
        if (users.isEmpty()) {
            users.add(User(1, "admin", "123456"))
            users.add(User(2, "seller", "123456"))
        }
        // 消息板块演示用的买家账号（已存在则跳过）
        ensureDemoUser("xiaomei", "小美")
        ensureDemoUser("ahao", "阿豪")
        if (goods.isEmpty()) {
            goods.add(
                Goods(1001, "小米电饭煲 4L", 189.0, "自用一年，功能正常，附赠蒸笼。",
                    Category.APPLIANCE, 2, "seller", false, now - 20 * 3600_000L)
            )
            goods.add(
                Goods(1002, "九阳豆浆机", 99.0, "闲置全新，原包装还在。",
                    Category.APPLIANCE, 1, "admin", false, now - 8 * 3600_000L)
            )
            goods.add(
                Goods(1003, "红富士苹果 5 斤", 29.9, "当季新鲜，脆甜多汁，宿舍自提。",
                    Category.FRUIT, 2, "seller", false, now - 5 * 3600_000L)
            )
            goods.add(
                Goods(1004, "海南香蕉 3 斤", 15.5, "自然熟，今天买多了。",
                    Category.FRUIT, 2, "seller", false, now - 3 * 3600_000L)
            )
            goods.add(
                Goods(1005, "红米 K70 手机壳", 19.9, "全新未拆封，黑色磨砂。",
                    Category.DIGITAL, 2, "seller", false, now - 90 * 60_000L)
            )
            goods.add(
                Goods(1006, "罗技无线鼠标", 45.0, "九成新，电池仓干净无漏液。",
                    Category.DIGITAL, 2, "seller", true, now - 120 * 60_000L)
            )
            goods.add(
                Goods(1007, "大学英语四级真题", 12.0, "做过两套，其余全新。",
                    Category.BOOK, 1, "admin", false, now - 200 * 60_000L)
            )
            goods.add(
                Goods(1008, "纯棉短袖 T 恤 L 码", 25.0, "买大了一码，只试穿过。",
                    Category.CLOTHING, 2, "seller", false, now - 240 * 60_000L)
            )
            goods.add(
                Goods(1009, "宿舍小台灯", 18.0, "USB 供电，三档亮度。",
                    Category.OTHER, 2, "seller", false, now - 300 * 60_000L)
            )
        }
        if (messages.isEmpty()) seedMessages(now)
        persistUsers()
        persistGoods()
        persistOrders()
        persistMessages()
    }

    private fun ensureDemoUser(username: String, nickname: String) {
        if (users.any { it.username == username }) return
        users.add(
            User(
                id = (users.maxOfOrNull { it.id } ?: 0) + 1,
                username = username,
                password = "123456",
                nickname = nickname
            )
        )
    }

    /** 预置几条聊天示例，方便演示消息板块（时间相对当前时间生成）。 */
    private fun seedMessages(now: Long) {
        val admin = users.find { it.username == "admin" } ?: return
        val seller = users.find { it.username == "seller" } ?: return

        fun add(
            from: User,
            to: User,
            goodsId: Int,
            goodsName: String,
            text: String,
            time: Long,
            read: Boolean
        ) {
            messages.add(
                ChatMessage(
                    id = messages.size + 1,
                    fromId = from.id,
                    fromName = from.displayName,
                    toId = to.id,
                    toName = to.displayName,
                    goodsId = goodsId,
                    goodsName = goodsName,
                    content = text,
                    time = time,
                    read = read
                )
            )
        }

        val pot = "小米电饭煲 4L"
        val phoneCase = "红米 K70 手机壳"
        val soyMilk = "九阳豆浆机"
        val book = "大学英语四级真题"

        add(admin, seller, 1001, pot, "你好，电饭煲还在吗？", now - 2 * 3600_000L, true)
        add(seller, admin, 1001, pot, "在的，功能正常，蒸笼也一起送。", now - 2 * 3600_000L + 3 * 60_000L, true)
        add(admin, seller, 1005, phoneCase, "手机壳还有全新的吗？", now - 40 * 60_000L, true)
        add(seller, admin, 1005, phoneCase, "有，两个一起还能便宜 5 元。", now - 35 * 60_000L, false)
        add(admin, seller, 1005, phoneCase, "好，我下午去宿舍楼下找你。", now - 30 * 60_000L, false)

        users.find { it.username == "xiaomei" }?.let { xiaomei ->
            add(xiaomei, admin, 1002, soyMilk, "学长，豆浆机还能便宜一点吗？", now - 26 * 3600_000L, true)
            add(admin, xiaomei, 1002, soyMilk, "最低 85，宿舍自提哦。", now - 25 * 3600_000L, true)
            add(xiaomei, admin, 1002, soyMilk, "成交！明天中午方便吗？", now - 24 * 3600_000L, false)
        }
        users.find { it.username == "ahao" }?.let { ahao ->
            add(ahao, admin, 1007, book, "四级真题还有吗？", now - 72 * 3600_000L, true)
            add(admin, ahao, 1007, book, "有的，做过的两套可以直接送你。", now - 71 * 3600_000L, true)
            add(ahao, admin, 1007, book, "那我要了，谢谢学长！", now - 70 * 3600_000L, false)
        }
    }
}
