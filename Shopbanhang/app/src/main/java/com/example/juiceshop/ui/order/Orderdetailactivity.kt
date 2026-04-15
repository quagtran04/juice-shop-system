package com.example.juiceshop.ui.order

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.juiceshop.R
import com.example.juiceshop.adapter.OrderDetailAdapter
import com.example.juiceshop.ui.cart.CartActivity
import com.example.juiceshop.ui.main.HomeActivity
import com.example.juiceshop.ui.product.SearchActivity
import com.example.juiceshop.ui.profile.ProfileActivity
import com.example.juiceshop.viewmodel.OrderViewModel

class OrderDetailActivity : AppCompatActivity() {

    private val viewModel: OrderViewModel by viewModels()
    private lateinit var adapter: OrderDetailAdapter

    private lateinit var rvOrderDetails:  RecyclerView
    private lateinit var tvOrderId:       TextView
    private lateinit var tvStatus:        TextView
    private lateinit var tvTotalAmount:   TextView
    private lateinit var tvOrderDate:     TextView
    private lateinit var tvPaymentStatus: TextView   // ← thêm mới
    private lateinit var progressBar:     View
    private lateinit var btnBack:         ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        bindViews()
        setupRecyclerView()
        setupNavigation()
        observeViewModel()

        val orderId = intent.getStringExtra("orderId") ?: run {
            finish()
            return
        }

        // Mã đơn
        tvOrderId.text = "#${orderId.takeLast(6).uppercase()}"

        // Ngày đặt
        val orderDate = intent.getStringExtra("orderDate") ?: ""
        tvOrderDate.text = orderDate.ifBlank { "Không có" }

        // Tổng tiền — format lại nếu là số, giữ nguyên nếu đã format
        val totalRaw = intent.getStringExtra("totalAmount") ?: ""
        tvTotalAmount.text = try {
            val amount = totalRaw.replace(",", "").replace(" đ", "").trim().toDouble()
            "${String.format("%,.0f", amount)} đ"
        } catch (e: Exception) {
            totalRaw.ifBlank { "Không có" }
        }

        // Trạng thái đơn hàng
        val status = intent.getStringExtra("status") ?: "pending"
        bindStatus(status)

        // Tình trạng thanh toán
        val paymentStatus = intent.getStringExtra("paymentStatus") ?: "unpaid"
        val paymentMethod = intent.getStringExtra("paymentMethod") ?: "cod"
        bindPaymentStatus(paymentStatus, paymentMethod)

        viewModel.loadOrderDetails(orderId)
    }

    private fun bindStatus(status: String) {
        val (label, color) = when (status.lowercase()) {
            "pending"    -> Pair("Chờ xác nhận", "#FF9800")
            "confirmed"  -> Pair("Đã xác nhận",  "#2196F3")
            "delivering" -> Pair("Đang giao",     "#9C27B0")
            "completed"  -> Pair("Hoàn thành",    "#4CAF50")
            "cancelled"  -> Pair("Đã hủy",        "#F44336")
            else         -> Pair(status,           "#888888")
        }
        tvStatus.text = label
        tvStatus.setTextColor(Color.parseColor(color))
    }

    private fun bindPaymentStatus(paymentStatus: String, paymentMethod: String) {
        val methodLabel = when (paymentMethod.lowercase()) {
            "momo" -> "MoMo"
            "cod"  -> "COD"
            else   -> paymentMethod.uppercase()
        }
        val (label, bgColor) = when (paymentStatus.lowercase()) {
            "paid"   -> Pair("✅ Đã TT · $methodLabel", "#4CAF50")
            "unpaid" -> Pair("💳 Chưa thanh toán · $methodLabel", "#FF9800")
            else     -> Pair(paymentStatus, "#888888")
        }
        tvPaymentStatus.text = label
        tvPaymentStatus.setBackgroundColor(Color.parseColor(bgColor))
    }

    private fun bindViews() {
        rvOrderDetails  = findViewById(R.id.rvOrderDetails)
        tvOrderId       = findViewById(R.id.tvOrderId)
        tvStatus        = findViewById(R.id.tvStatus)
        tvTotalAmount   = findViewById(R.id.tvTotalAmount)
        tvOrderDate     = findViewById(R.id.tvOrderDate)
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus)   // ← thêm mới
        progressBar     = findViewById(R.id.progressBar)
        btnBack         = findViewById(R.id.btnBack)
    }

    private fun setupRecyclerView() {
        adapter = OrderDetailAdapter()
        rvOrderDetails.layoutManager = LinearLayoutManager(this)
        rvOrderDetails.adapter = adapter
    }

    private fun setupNavigation() {
        btnBack.setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.orderDetails.observe(this) { details ->
            adapter.submitList(details)
        }
        viewModel.error.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }
}