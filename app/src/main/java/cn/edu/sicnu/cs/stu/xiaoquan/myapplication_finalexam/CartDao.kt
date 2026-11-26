package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

interface CartDao {
    suspend fun addToCart(goodsId: Int, buyerId: Int): Boolean
    suspend fun getCartItems(buyerId: Int): List<CartItem>
    suspend fun getCartGoods(buyerId: Int): List<Goods>
    suspend fun removeFromCart(goodsId: Int, buyerId: Int)
    suspend fun cartCount(buyerId: Int): Int
}
