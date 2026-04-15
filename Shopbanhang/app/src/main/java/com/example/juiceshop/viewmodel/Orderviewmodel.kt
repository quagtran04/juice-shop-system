package com.example.juiceshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juiceshop.data.OrderRepository
import com.example.juiceshop.model.Order
import com.example.juiceshop.model.OrderDetail
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {

    private val orderRepository = OrderRepository()

    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> = _orders

    private val _orderDetails = MutableLiveData<List<OrderDetail>>()
    val orderDetails: LiveData<List<OrderDetail>> = _orderDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadUserOrders() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _error.value = "User not logged in"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _orders.value = orderRepository.getOrdersByUser(uid)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _orderDetails.value = orderRepository.getOrderDetails(orderId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}