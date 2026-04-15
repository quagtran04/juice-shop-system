package com.example.juiceshop.ui.product

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.juiceshop.R
import com.example.juiceshop.adapter.ProductSearchAdapter
import com.example.juiceshop.ui.cart.CartActivity
import com.example.juiceshop.ui.main.HomeActivity
import com.example.juiceshop.ui.order.OrderListActivity
import com.example.juiceshop.ui.profile.ProfileActivity
import com.example.juiceshop.viewmodel.ProductViewModel

class SearchActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductSearchAdapter

    private lateinit var etSearch: EditText
    private lateinit var rvProducts: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: View
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        bindViews()
        setupRecyclerView()
        setupNavigation()
        observeViewModel()
        setupSearchListener()

        // Auto-focus search box
        etSearch.requestFocus()
    }

    private fun bindViews() {
        etSearch    = findViewById(R.id.etSearch)
        rvProducts  = findViewById(R.id.rvProducts)
        tvEmpty     = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        btnBack     = findViewById(R.id.btnBack)
    }

    private fun setupRecyclerView() {
        adapter = ProductSearchAdapter { product ->
            startActivity(
                Intent(this, ProductDetailActivity::class.java)
                    .putExtra("productId", product.productId)
            )
        }
        rvProducts.layoutManager = GridLayoutManager(this, 2)
        rvProducts.adapter = adapter
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchProducts(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupNavigation() {
        btnBack.setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
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
            adapter.submitList(list)
            tvEmpty.visibility = if (list.isEmpty() && etSearch.text.isNotBlank())
                View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }
}