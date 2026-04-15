package com.example.juiceshop.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.juiceshop.R
import com.example.juiceshop.adapter.ProductAdapter
import com.example.juiceshop.ui.cart.CartActivity
import com.example.juiceshop.ui.main.HomeActivity
import com.example.juiceshop.ui.order.OrderListActivity
import com.example.juiceshop.ui.profile.ProfileActivity
import com.example.juiceshop.viewmodel.ProductViewModel

class CategoryActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter

    private lateinit var rvProducts: RecyclerView
    private lateinit var tvCategoryName: TextView
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        bindViews()
        setupRecyclerView()
        setupNavigation()
        observeViewModel()

        val categoryId   = intent.getStringExtra("categoryId") ?: return
        val categoryName = intent.getStringExtra("categoryName") ?: ""
        tvCategoryName.text = categoryName

        viewModel.loadProductsByCategory(categoryId)
    }

    private fun bindViews() {
        rvProducts     = findViewById(R.id.rvProducts)
        tvCategoryName = findViewById(R.id.tvCategoryName)
        btnBack        = findViewById(R.id.btnBack)
        progressBar    = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter { product ->
            startActivity(
                Intent(this, ProductDetailActivity::class.java)
                    .putExtra("productId", product.productId)
            )
        }
        rvProducts.layoutManager = GridLayoutManager(this, 2)
        rvProducts.adapter = adapter
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
        findViewById<ImageButton>(R.id.btnOrders).setOnClickListener {
            startActivity(Intent(this, OrderListActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.products.observe(this) { list ->
            if (list.isEmpty()) {
                Toast.makeText(this, "No products found in this category", Toast.LENGTH_SHORT).show()
            }
            adapter.submitList(list)
        }

        viewModel.error.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }
}