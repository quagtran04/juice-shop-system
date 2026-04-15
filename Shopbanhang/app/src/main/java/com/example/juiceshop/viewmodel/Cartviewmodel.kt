package com.example.juiceshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juiceshop.data.CartManager
import com.example.juiceshop.data.CartRepository
import com.example.juiceshop.data.OrderRepository
import com.example.juiceshop.model.CartItem
import com.example.juiceshop.model.Order
import com.example.juiceshop.model.OrderDetail
import com.example.juiceshop.model.Product
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CartViewModel : ViewModel() {

    private val cartManager    = CartManager.getInstance()
    private val cartRepository = CartRepository()
    private val orderRepository = OrderRepository()

    private val _cartItems   = MutableLiveData<List<CartItem>>()
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _totalAmount = MutableLiveData<Double>()
    val totalAmount: LiveData<Double> = _totalAmount

    private val _orderState  = MutableLiveData<OrderState>()
    val orderState: LiveData<OrderState> = _orderState

    private val _isLoading   = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init { loadCartFromFirestore() }

    // Load giỏ hàng từ Firestore khi khởi động

    private fun loadCartFromFirestore() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val items = cartRepository.loadCart()
                cartManager.setItems(items)
                refreshCart()
            } catch (e: Exception) {
                // Nếu load lỗi thì dùng cache local
                refreshCart()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshCart() {
        _cartItems.value   = cartManager.getCartItems()
        _totalAmount.value = cartManager.getTotalAmount()
    }

    // Thêm sản phẩm vào giỏ (gọi từ ProductDetailActivity)

    fun addToCart(product: Product) {
        val item = cartManager.addItem(product)
        refreshCart()
        viewModelScope.launch {
            try { cartRepository.saveItem(item) } catch (_: Exception) {}
        }
    }

    // Xóa item

    fun removeItem(position: Int) {
        val productId = cartManager.removeItem(position) ?: return
        refreshCart()
        viewModelScope.launch {
            try { cartRepository.removeItem(productId) } catch (_: Exception) {}
        }
    }

    // Cập nhật số lượng

    fun updateQuantity(position: Int, quantity: Int) {
        val item = cartManager.updateQuantity(position, quantity)
        refreshCart()
        viewModelScope.launch {
            try {
                if (item != null) cartRepository.saveItem(item)
                else {
                    // quantity <= 0 → đã xóa → lấy productId từ Firestore không cần thiết
                    // CartManager đã xóa rồi, Firestore sync qua removeItem
                }
            } catch (_: Exception) {}
        }
    }

    // Đặt hàng

    fun placeOrder(
        customerName:  String,
        address:       String,
        phone:         String,
        finalTotal:    Double = cartManager.getTotalAmount(),
        selectedItems: List<CartItem> = cartManager.getCartItems()
    ) {
        val items = selectedItems

        when {
            customerName.isBlank() ->
                _orderState.value = OrderState.Error("Vui lòng nhập họ tên")
            address.isBlank() ->
                _orderState.value = OrderState.Error("Vui lòng nhập địa chỉ")
            phone.isBlank() ->
                _orderState.value = OrderState.Error("Vui lòng nhập số điện thoại")
            items.isEmpty() ->
                _orderState.value = OrderState.Error("Giỏ hàng trống")
            else -> {
                _orderState.value = OrderState.Loading
                viewModelScope.launch {
                    try {
                        val uid     = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val dateStr = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                        ).format(Date())

                        val details = items.map { item ->
                            OrderDetail(
                                productId   = item.product.productId,
                                productName = item.product.name,
                                imageUrl    = item.product.imageUrl,
                                quantity    = item.quantity,
                                unitPrice   = item.product.price
                            )
                        }

                        val order = Order(
                            userId       = uid,
                            customerName = customerName,
                            address      = address,
                            phone        = phone,
                            totalAmount  = finalTotal,
                            orderDate    = dateStr
                        )

                        val orderId = orderRepository.placeOrder(order, details)

                        // Chỉ xóa sản phẩm được tick, giữ lại phần còn lại
                        selectedItems.forEach { item ->
                            cartManager.removeItemById(item.product.productId)
                            cartRepository.removeItem(item.product.productId)
                        }
                        refreshCart()

                        _orderState.value = OrderState.Success(orderId)

                    } catch (e: Exception) {
                        _orderState.value = OrderState.Error(e.message ?: "Đặt hàng thất bại")
                    }
                }
            }
        }
    }

    sealed class OrderState {
        object Loading : OrderState()
        data class Success(val orderId: String) : OrderState()
        data class Error(val message: String) : OrderState()
    }
}