package com.example.juiceshop.data

import com.example.juiceshop.model.CartItem
import com.example.juiceshop.model.Product

class CartManager private constructor() {

    private val items: MutableList<CartItem> = mutableListOf()

    companion object {
        @Volatile
        private var instance: CartManager? = null

        fun getInstance(): CartManager =
            instance ?: synchronized(this) {
                instance ?: CartManager().also { instance = it }
            }
    }

    // ── Cache ops (không gọi Firestore)

    fun getCartItems(): List<CartItem> = items.map { it.copy() }

    fun setItems(newItems: List<CartItem>) {
        items.clear()
        items.addAll(newItems)
    }

    fun addItem(product: Product): CartItem {
        val existing = items.find { it.product.productId == product.productId }
        return if (existing != null) {
            existing.quantity += 1
            existing.copy()
        } else {
            val newItem = CartItem(product, 1)
            items.add(newItem)
            newItem.copy()
        }
    }

    fun removeItem(position: Int): String? {
        if (position !in items.indices) return null
        val productId = items[position].product.productId
        items.removeAt(position)
        return productId
    }

    fun updateQuantity(position: Int, quantity: Int): CartItem? {
        if (position !in items.indices) return null
        return if (quantity <= 0) {
            items.removeAt(position)
            null
        } else {
            items[position].quantity = quantity
            items[position].copy()
        }
    }

    fun removeItemById(productId: String) {
        items.removeAll { it.product.productId == productId }
    }

    fun getTotalAmount(): Double = items.sumOf { it.getTotalPrice() }
    fun getItemCount(): Int     = items.sumOf { it.quantity }
    fun clearCache()            = items.clear()
}