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
object Repository : UserDao, GoodsDao, OrderDao {

    private val lock = Any()
    private lateinit var dir: File
    private val users = ArrayList<User>()
    private val goods = ArrayList<Goods>()
    private val orders = ArrayList<Order>()
    private var initialized = false

    private val usersFile: File get() = File(dir, "users.dat")
    private val goodsFile: File get() = File(dir, "goods.dat")
    private val ordersFile: File get() = File(dir, "orders.dat")

    fun init(context: Context) {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            dir = File(context.applicationContext.filesDir, "shop").apply { mkdirs() }
            users.addAll(readList(usersFile))
            goods.addAll(readList(goodsFile))
            orders.addAll(readList(ordersFile))
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
            persistGoods()
            persistOrders()
            true
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

    private fun seedIfEmpty() {
        val now = System.currentTimeMillis()
        if (users.isEmpty()) {
            users.add(User(1, "admin", "123456"))
            users.add(User(2, "seller", "123456"))
        }
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
        persistUsers()
        persistGoods()
        persistOrders()
    }
}
