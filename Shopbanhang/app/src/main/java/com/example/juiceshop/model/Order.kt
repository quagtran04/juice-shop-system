package com.example.juiceshop.model

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val customerName: String = "",
    val address: String = "",
    val phone: String = "",
    val totalAmount: Double = 0.0,
    val orderDate: String = "",
    val status: String = "pending",        // pending | confirmed | delivering | completed | cancelled
    val paymentStatus: String = "unpaid",  // unpaid | paid
    val paymentMethod: String = "cod",     // cod | momo
    val details: List<OrderDetail> = emptyList()
) {
    constructor() : this("", "", "", "", "", 0.0, "", "pending", "unpaid", "cod", emptyList())
}