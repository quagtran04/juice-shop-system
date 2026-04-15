package com.example.juiceshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juiceshop.data.ProductRepository
import com.example.juiceshop.model.Product
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val repository = ProductRepository()

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Tất cả sản phẩm
    fun loadAllProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _products.value = repository.getAllProducts()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Sản phẩm theo danh mục
    fun loadProductsByCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _products.value = repository.getProductsByCategory(categoryId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Chi tiết sản phẩm
    fun loadProductDetail(productId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _product.value = repository.getProductById(productId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Tìm kiếm
    fun searchProducts(keyword: String) {
        if (keyword.isBlank()) {
            _products.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _products.value = repository.searchProductsByName(keyword)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}