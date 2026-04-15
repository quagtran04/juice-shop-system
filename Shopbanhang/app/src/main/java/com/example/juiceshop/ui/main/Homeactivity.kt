package com.example.juiceshop.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.juiceshop.R
import com.example.juiceshop.adapter.CategoryAdapter
import com.example.juiceshop.adapter.ProductAdapter
import com.example.juiceshop.adapter.ProductNewAdapter
import com.example.juiceshop.model.Category
import com.example.juiceshop.model.Product
import com.example.juiceshop.ui.cart.CartActivity
import com.example.juiceshop.ui.chat.ChatActivity
import com.example.juiceshop.ui.order.OrderListActivity
import com.example.juiceshop.ui.product.CategoryActivity
import com.example.juiceshop.ui.product.ProductDetailActivity
import com.example.juiceshop.ui.product.ProductListActivity
import com.example.juiceshop.ui.product.SearchActivity
import com.example.juiceshop.ui.profile.ProfileActivity
import com.example.juiceshop.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()

    // Views
    private lateinit var tvUsername: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var rvCategories: RecyclerView
    private lateinit var rvNewProducts: RecyclerView
    private lateinit var rvBestSellers: RecyclerView
    private lateinit var rvAllProducts: RecyclerView
    private lateinit var progressBar: View
    private lateinit var etSearch: EditText
    private lateinit var tvViewAll: TextView

    // Bottom nav
    private lateinit var btnHome: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var btnCart: ImageButton
    private lateinit var btnOrders: ImageButton
    private lateinit var btnProfile: ImageButton

    // FAB Chat
    private lateinit var fabChat: ImageView

    // Adapters
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var newProductAdapter: ProductNewAdapter
    private lateinit var bestSellerAdapter: ProductNewAdapter
    private lateinit var allProductAdapter: ProductAdapter

    // Slideshow
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var currentPage = 0
    private val adImages = intArrayOf(
        R.drawable.sl6, R.drawable.sl2, R.drawable.sl1, R.drawable.sl3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bindViews()
        setupAdapters()
        setupRecyclerViews()
        setupSlideshow()
        loadUsername()
        setListeners()
        observeViewModel()
    }

    private fun bindViews() {
        tvUsername      = findViewById(R.id.tvUsername)
        viewPager       = findViewById(R.id.viewPager)
        rvCategories    = findViewById(R.id.rvCategories)
        rvNewProducts   = findViewById(R.id.rvNewProducts)
        rvBestSellers   = findViewById(R.id.rvBestSellers)
        rvAllProducts   = findViewById(R.id.rvAllProducts)
        progressBar     = findViewById(R.id.progressBar)
        etSearch        = findViewById(R.id.etSearch)
        tvViewAll       = findViewById(R.id.tvViewAll)
        btnHome         = findViewById(R.id.btnHome)
        btnSearch       = findViewById(R.id.btnSearch)
        btnCart         = findViewById(R.id.btnCart)
        btnOrders       = findViewById(R.id.btnOrders)
        btnProfile      = findViewById(R.id.btnProfile)
        // FAB
        fabChat         = findViewById(R.id.fabChat)
    }

    private fun setupAdapters() {
        categoryAdapter = CategoryAdapter { category ->
            startActivity(
                Intent(this, CategoryActivity::class.java)
                    .putExtra("categoryId", category.categoryId)
                    .putExtra("categoryName", category.name)
            )
        }

        newProductAdapter = ProductNewAdapter { product -> openProductDetail(product) }
        bestSellerAdapter = ProductNewAdapter { product -> openProductDetail(product) }
        allProductAdapter = ProductAdapter { product -> openProductDetail(product) }
    }

    private fun setupRecyclerViews() {
        rvCategories.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCategories.adapter = categoryAdapter

        rvNewProducts.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvNewProducts.adapter = newProductAdapter

        rvBestSellers.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvBestSellers.adapter = bestSellerAdapter

        rvAllProducts.layoutManager = GridLayoutManager(this, 3)
        rvAllProducts.adapter = allProductAdapter
    }

    private fun setupSlideshow() {
        viewPager.adapter = SlideshowAdapter(adImages)
        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            currentPage = (currentPage + 1) % adImages.size
            viewPager.setCurrentItem(currentPage, true)
            handler.postDelayed(runnable, 4000)
        }
        handler.postDelayed(runnable, 4000)
    }

    private fun loadUsername() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                tvUsername.text = doc.getString("username") ?: ""
            }
    }

    private fun setListeners() {
        tvViewAll.setOnClickListener {
            startActivity(Intent(this, ProductListActivity::class.java))
        }

        etSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        btnCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        btnOrders.setOnClickListener {
            startActivity(Intent(this, OrderListActivity::class.java))
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnHome.setOnClickListener { /* Already on home */ }

        // Mở ChatActivity khi nhấn FAB
        fabChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.categories.observe(this) { list ->
            categoryAdapter.submitList(list)
        }

        viewModel.newProducts.observe(this) { list ->
            newProductAdapter.submitList(list)
        }

        viewModel.bestSellerProducts.observe(this) { list ->
            bestSellerAdapter.submitList(list)
        }

        viewModel.randomProducts.observe(this) { list ->
            allProductAdapter.submitList(list)
        }

        viewModel.error.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun openProductDetail(product: Product) {
        startActivity(
            Intent(this, ProductDetailActivity::class.java)
                .putExtra("productId", product.productId)
        )
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runnable, 4000)
    }

    // Inner Slideshow Adapter
    private inner class SlideshowAdapter(private val images: IntArray) :
        RecyclerView.Adapter<SlideshowAdapter.SlideViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_slide, parent, false)
            return SlideViewHolder(view)
        }

        override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
            holder.imageView.setImageResource(images[position])
        }

        override fun getItemCount(): Int = images.size

        inner class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.ivSlide)
        }
    }
}