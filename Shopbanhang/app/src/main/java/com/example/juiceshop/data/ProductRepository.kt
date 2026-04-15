package com.example.juiceshop.data

import com.example.juiceshop.model.Category
import com.example.juiceshop.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProductRepository {

    private val db            = FirebaseFirestore.getInstance()
    private val productsRef   = db.collection("products")
    private val categoriesRef = db.collection("categories")

    // CATEGORY

    suspend fun getAllCategories(): List<Category> {
        return categoriesRef.get().await()
            .documents
            .mapNotNull { it.toObject(Category::class.java)?.copy(categoryId = it.id) }
    }

    // PRODUCT

    suspend fun getAllProducts(): List<Product> {
        return productsRef.get().await()
            .documents
            .mapNotNull { it.toObject(Product::class.java)?.copy(productId = it.id) }
    }

    suspend fun getProductsByCategory(categoryId: String): List<Product> {
        return productsRef
            .whereEqualTo("categoryId", categoryId)
            .get().await()
            .documents
            .mapNotNull { it.toObject(Product::class.java)?.copy(productId = it.id) }
    }

    suspend fun getProductById(productId: String): Product? {
        val doc = productsRef.document(productId).get().await()
        return doc.toObject(Product::class.java)?.copy(productId = doc.id)
    }

    suspend fun searchProductsByName(keyword: String): List<Product> {
        val keywordLower = keyword.lowercase().trim()
        return productsRef.get().await()
            .documents
            .mapNotNull { it.toObject(Product::class.java)?.copy(productId = it.id) }
            .filter { it.name.lowercase().contains(keywordLower) }
    }

    // Sản phẩm mới nhất: sắp xếp theo createdAt giảm dần
    suspend fun getNewestProducts(limit: Long = 10): List<Product> {
        return productsRef.get().await()
            .documents
            .mapNotNull { it.toObject(Product::class.java)?.copy(productId = it.id) }
            .sortedByDescending { it.createdAt?.time ?: 0L }
            .take(limit.toInt())
    }

    // Sản phẩm bán chạy: sắp xếp theo soldCount giảm dần
    suspend fun getBestSellerProducts(limit: Long = 10): List<Product> {
        return productsRef.get().await()
            .documents
            .mapNotNull { it.toObject(Product::class.java)?.copy(productId = it.id) }
            .sortedByDescending { it.soldCount }
            .take(limit.toInt())
    }
}