package com.example.juiceshop.ui.product

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.juiceshop.R
import com.example.juiceshop.model.Product
import com.example.juiceshop.ui.cart.CartActivity
import com.example.juiceshop.ui.main.HomeActivity
import com.example.juiceshop.ui.order.OrderListActivity
import com.example.juiceshop.ui.profile.ProfileActivity
import com.example.juiceshop.viewmodel.CartViewModel
import com.example.juiceshop.viewmodel.ProductViewModel

class ProductDetailActivity : AppCompatActivity() {

    private val productViewModel: ProductViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()   // ← thêm CartViewModel

    private lateinit var ivProduct:     ImageView
    private lateinit var tvName:        TextView
    private lateinit var tvPrice:       TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvNote:        TextView
    private lateinit var tvStock:       TextView
    private lateinit var btnAddToCart:  Button
    private lateinit var btnBuyNow:     Button
    private lateinit var btnBack:       ImageView
    private lateinit var progressBar:   View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        bindViews()
        setupNavigation()
        observeViewModel()

        val productId = intent.getStringExtra("productId") ?: run {
            finish()
            return
        }
        productViewModel.loadProductDetail(productId)
    }

    private fun bindViews() {
        ivProduct     = findViewById(R.id.ivProduct)
        tvName        = findViewById(R.id.tvName)
        tvPrice       = findViewById(R.id.tvPrice)
        tvDescription = findViewById(R.id.tvDescription)
        tvNote        = findViewById(R.id.tvNote)
        tvStock       = findViewById(R.id.tvStock)
        btnAddToCart  = findViewById(R.id.btnAddToCart)
        btnBuyNow     = findViewById(R.id.btnBuyNow)
        btnBack       = findViewById(R.id.btnBack)
        progressBar   = findViewById(R.id.progressBar)
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
        productViewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        productViewModel.product.observe(this) { product ->
            product?.let { bindProduct(it) }
        }

        productViewModel.error.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun bindProduct(product: Product) {
        tvName.text        = product.name
        tvPrice.text       = "${String.format("%,.0f", product.price)} VNĐ"  // ← fix format giá
        tvDescription.text = product.description.ifBlank { "Chưa có mô tả" }
        tvNote.text        = product.note.ifBlank { "Chưa có ghi chú" }
        tvStock.text       = "Còn lại: ${product.stock}"

        Glide.with(this)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .into(ivProduct)

        btnAddToCart.setOnClickListener {
            cartViewModel.addToCart(product)
            Toast.makeText(this, "Đã thêm vào giỏ hàng! 🛒", Toast.LENGTH_SHORT).show()
        }

        btnBuyNow.setOnClickListener {
            cartViewModel.addToCart(product)
            startActivity(Intent(this, CartActivity::class.java))
        }
    }
}