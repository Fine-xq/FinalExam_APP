package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import java.io.Serializable

/**
 * 订单实体：一条记录同时保存买家与卖家，便于分别查看"我买到的"和"我卖出的"。
 */
data class Order(
    val id: Int,
    val goodsId: Int,
    val goodsName: String,
    val price: Double,
    val buyerId: Int,
    val buyerName: String,
    val sellerId: Int,
    val sellerName: String,
    val time: Long
) : Serializable
