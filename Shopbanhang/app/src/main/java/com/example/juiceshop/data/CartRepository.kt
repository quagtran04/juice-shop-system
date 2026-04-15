package com.example.juiceshop.data

import com.example.juiceshop.model.CartItem
import com.example.juiceshop.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class CartRepository {

    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    private fun itemsRef() = uid?.let {
        db.collection("carts").document(it).collection("items")
    }

    // Đọc giỏ hàng từ Firestore

    suspend fun loadCart(): List<CartItem> {
        val ref = itemsRef() ?: return emptyList()
        return ref.get().await().documents.mapNotNull { doc ->
            val productId   = doc.getString("productId")   ?: return@mapNotNull null
            val name        = doc.getString("name")        ?: ""
            val price       = doc.getDouble("price")       ?: 0.0
            val imageUrl    = doc.getString("imageUrl")    ?: ""
            val description = doc.getString("description") ?: ""
            val note        = doc.getString("note")        ?: ""
            val stock       = (doc.getLong("stock")        ?: 0L).toInt()
            val categoryId  = doc.getString("categoryId")  ?: ""
            val quantity    = (doc.getLong("quantity")     ?: 1L).toInt()

            CartItem(
                product  = Product(
                    productId   = productId,
                    name        = name,
                    price       = price,
                    imageUrl    = imageUrl,
                    description = description,
                    note        = note,
                    stock       = stock,
                    categoryId  = categoryId
                ),
                quantity = quantity
            )
        }
    }

    // Thêm / cập nhật 1 item

    suspend fun saveItem(item: CartItem) {
        val ref = itemsRef() ?: return
        val data = mapOf(
            "productId"   to item.product.productId,
            "name"        to item.product.name,
            "price"       to item.product.price,
            "imageUrl"    to item.product.imageUrl,
            "description" to item.product.description,
            "note"        to item.product.note,
            "stock"       to item.product.stock,
            "categoryId"  to item.product.categoryId,
            "quantity"    to item.quantity
        )
        ref.document(item.product.productId).set(data, SetOptions.merge()).await()
    }

    // Xóa 1 item

    suspend fun removeItem(productId: String) {
        itemsRef()?.document(productId)?.delete()?.await()
    }

    // Xóa toàn bộ giỏ hàng

    suspend fun clearCart() {
        val ref = itemsRef() ?: return
        val docs = ref.get().await().documents
        val batch = db.batch()
        docs.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}