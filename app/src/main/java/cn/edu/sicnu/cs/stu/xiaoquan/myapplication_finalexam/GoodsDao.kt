package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

interface GoodsDao {
    suspend fun publishGoods(goods: Goods): Boolean
    suspend fun updateGoods(goods: Goods, sellerId: Int): Boolean
    suspend fun deleteGoods(goodsId: Int, sellerId: Int): Boolean
    suspend fun getAllGoods(): List<Goods>
    suspend fun getMyGoods(sellerId: Int): List<Goods>
    suspend fun getGoodsById(goodsId: Int): Goods?
    suspend fun buyGoods(goodsId: Int, buyerId: Int, buyerName: String): Boolean
}
