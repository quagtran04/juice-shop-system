package com.example.juiceshop.data

import com.example.juiceshop.model.Order
import com.example.juiceshop.model.OrderDetail
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class OrderRepository {

    private val db = FirebaseFirestore.getInstance()
    private val ordersRef = db.collection("orders")
    private val productsRef = db.collection("products")

    suspend fun placeOrder(
        order: Order,
        details: List<OrderDetail>,
        paymentMethod: String = "cod"
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // COD → unpaid, MoMo → paid (callback sẽ xác nhận sau)
        val paymentStatus = "unpaid" // luôn unpaid khi tạo, callback MoMo mới đổi thành paid

        val orderData = order.copy(
            orderDate     = dateStr,
            status        = "pending",
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus
        )

        val orderDoc = ordersRef.add(orderData).await()
        val orderId  = orderDoc.id

        val batch = db.batch()
        details.forEach { detail ->
            val detailRef = ordersRef.document(orderId)
                .collection("orderDetails")
                .document()
            batch.set(detailRef, detail.copy(orderId = orderId, detailId = detailRef.id))

          val productRef = productsRef.document(detail.productId)
            batch.update(productRef, "stock",     FieldValue.increment(-detail.quantity.toLong()))
            batch.update(productRef, "soldCount", FieldValue.increment(detail.quantity.toLong()))
        }
        batch.commit().await()

        return orderId
    }

    suspend fun getOrdersByUser(userId: String): List<Order> {
        val orders = ordersRef
            .whereEqualTo("userId", userId)
            .orderBy("orderDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await()
            .documents
            .mapNotNull { it.toObject(Order::class.java)?.copy(orderId = it.id) }

        return orders.map { order ->
            val details = getOrderDetails(order.orderId)
            order.copy(details = details)
        }
    }

    suspend fun getOrderDetails(orderId: String): List<OrderDetail> {
        return ordersRef.document(orderId)
            .collection("orderDetails")
            .get().await()
            .documents
            .mapNotNull { it.toObject(OrderDetail::class.java)?.copy(detailId = it.id) }
    }
}