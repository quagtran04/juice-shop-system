package com.example.juiceshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juiceshop.data.ProductRepository
import com.example.juiceshop.model.Category
import com.example.juiceshop.model.Product
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val productRepository = ProductRepository()

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _newProducts = MutableLiveData<List<Product>>()
    val newProducts: LiveData<List<Product>> = _newProducts

    private val _bestSellerProducts = MutableLiveData<List<Product>>()
    val bestSellerProducts: LiveData<List<Product>> = _bestSellerProducts

    private val _randomProducts = MutableLiveData<List<Product>>()
    val randomProducts: LiveData<List<Product>> = _randomProducts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _categories.value       = productRepository.getAllCategories().shuffled().take(10)
                _newProducts.value      = productRepository.getNewestProducts(8)
                _bestSellerProducts.value = productRepository.getBestSellerProducts(8)
                _randomProducts.value   = productRepository.getAllProducts().shuffled().take(18)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}