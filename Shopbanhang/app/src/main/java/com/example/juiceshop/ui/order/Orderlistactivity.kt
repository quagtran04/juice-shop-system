package com.example.juiceshop.ui.order

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.juiceshop.R
import com.example.juiceshop.adapter.OrderAdapter
import com.example.juiceshop.ui.cart.CartActivity
import com.example.juiceshop.ui.main.HomeActivity
import com.example.juiceshop.ui.product.SearchActivity
import com.example.juiceshop.ui.profile.ProfileActivity
import com.example.juiceshop.viewmodel.OrderViewModel

class OrderListActivity : AppCompatActivity() {

    private val viewModel: OrderViewModel by viewModels()
    private lateinit var adapter: OrderAdapter

    private lateinit var rvOrders:   RecyclerView
    private lateinit var tvEmpty:    TextView
    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_list)

        bindViews()
        setupRecyclerView()
        setupNavigation()
        observeViewModel()

        viewModel.loadUserOrders()
    }

    private fun bindViews() {
        rvOrders    = findViewById(R.id.rvOrders)
        tvEmpty     = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter { order ->
            startActivity(
                Intent(this, OrderDetailActivity::class.java)
                    .putExtra("orderId",       order.orderId)
                    .putExtra("orderDate",     order.orderDate)
                    .putExtra("totalAmount",   order.totalAmount.toString())
                    .putExtra("status",        order.status)
                    .putExtra("paymentStatus", order.paymentStatus)
                    .putExtra("paymentMethod",  order.paymentMethod)
            )
        }
        rvOrders.layoutManager = LinearLayoutManager(this)
        rvOrders.adapter = adapter
    }

    private fun setupNavigation() {
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
        viewModel.orders.observe(this) { orders ->
            adapter.submitList(orders)
            tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserOrders()
    }
}