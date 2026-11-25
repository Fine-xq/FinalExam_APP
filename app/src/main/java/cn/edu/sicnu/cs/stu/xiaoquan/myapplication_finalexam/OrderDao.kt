package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

interface OrderDao {
    suspend fun getOrdersBought(userId: Int): List<Order>
    suspend fun getOrdersSold(userId: Int): List<Order>
}
