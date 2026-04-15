package com.example.juiceshop.ui.cart

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.juiceshop.R
import com.example.juiceshop.adapter.CartAdapter
import com.example.juiceshop.ui.main.HomeActivity
import com.example.juiceshop.ui.order.OrderListActivity
import com.example.juiceshop.ui.product.SearchActivity
import com.example.juiceshop.ui.profile.ProfileActivity
import com.example.juiceshop.viewmodel.CartViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class CartActivity : AppCompatActivity() {

    private val viewModel: CartViewModel by viewModels()
    private lateinit var adapter: CartAdapter

    private lateinit var rvCart:        RecyclerView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnCheckout:   Button
    private lateinit var layoutEmpty:   View
    private lateinit var progressBar:   View

    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    private var discountPercent = 0.0
    private var maxDiscount     = 0.0

    private val BACKEND_URL = "https://milled-lethally-pearline.ngrok-free.dev"

    private val momoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        progressBar.visibility = View.GONE
        btnCheckout.isEnabled  = true
        when (result.resultCode) {
            MomoWebViewActivity.RESULT_SUCCESS -> {
                Toast.makeText(this, "✅ Thanh toán MoMo thành công! 🎉", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
            }
            MomoWebViewActivity.RESULT_FAILED -> {
                Toast.makeText(this, "❌ Thanh toán MoMo thất bại hoặc bị hủy", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        bindViews()
        setupRecyclerView()
        setupNavigation()
        observeViewModel()

    }

    private fun bindViews() {
        rvCart        = findViewById(R.id.rvCart)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        btnCheckout   = findViewById(R.id.btnCheckout)
        layoutEmpty   = findViewById(R.id.layoutEmpty)
        progressBar   = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            onRemove = { position ->
                viewModel.removeItem(position)  // ← tự sync Firestore trong ViewModel
            },
            onQuantityChange = { position, qty ->
                viewModel.updateQuantity(position, qty)  // ← tự sync Firestore trong ViewModel
            },
            onSelectionChanged = {
                // Cập nhật tổng tiền theo sản phẩm được tick
                val selected = adapter.getSelectedTotal()
                tvTotalAmount.text = "${String.format("%,.0f", selected)} đ"
                btnCheckout.isEnabled = adapter.getSelectedItems().isNotEmpty()
            }
        )
        rvCart.layoutManager = LinearLayoutManager(this)
        rvCart.adapter = adapter
    }

    private fun setupNavigation() {
        btnCheckout.setOnClickListener { showPaymentDialog() }
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnOrders).setOnClickListener {
            startActivity(Intent(this, OrderListActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.cartItems.observe(this) { items ->
            adapter.submitList(items.toMutableList())
            layoutEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            btnCheckout.isEnabled  = items.isNotEmpty()
        }
        viewModel.totalAmount.observe(this) { total ->
            tvTotalAmount.text = "${String.format("%,.0f", total)} đ"
        }
        viewModel.orderState.observe(this) { state ->
            when (state) {
                is CartViewModel.OrderState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnCheckout.isEnabled  = false
                }
                is CartViewModel.OrderState.Success -> {}
                is CartViewModel.OrderState.Error -> {
                    progressBar.visibility = View.GONE
                    btnCheckout.isEnabled  = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Voucher từ Firestore

    private fun applyVoucherFromFirebase(
        code: String,
        originalTotal: Double,
        onResult: (discountAmount: Double, message: String, success: Boolean) -> Unit
    ) {
        db.collection("vouchers")
            .whereEqualTo("code", code)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onResult(0.0, "❌ Mã voucher không hợp lệ hoặc đã bị tắt", false)
                    return@addOnSuccessListener
                }
                val doc       = snap.documents[0]
                val expiryStr = doc.getString("expiryDate") ?: ""
                val pct       = (doc.getLong("discountPercent") ?: 0L).toDouble()
                val maxDis    = (doc.getLong("maxDiscount") ?: 0L).toDouble()
                val expiry    = try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryStr)
                } catch (e: Exception) { null }
                if (expiry == null || expiry.before(Date())) {
                    onResult(0.0, "❌ Voucher đã hết hạn", false)
                    return@addOnSuccessListener
                }
                val rawDiscount    = originalTotal * (pct / 100.0)
                val actualDiscount = if (maxDis > 0) minOf(rawDiscount, maxDis) else rawDiscount
                discountPercent    = pct / 100.0
                this.maxDiscount   = maxDis
                onResult(actualDiscount, "✅ Giảm ${pct.toInt()}%, tối đa ${String.format("%,.0f", maxDis)} đ", true)
            }
            .addOnFailureListener { onResult(0.0, "❌ Lỗi kết nối, thử lại!", false) }
    }

    // MoMo

    private fun createMomoPayment(
        orderId: String,
        amount: Long,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conn = (URL("$BACKEND_URL/api/momo/create").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("ngrok-skip-browser-warning", "true")
                    doOutput = true; connectTimeout = 15000; readTimeout = 15000
                }
                OutputStreamWriter(conn.outputStream).use {
                    it.write(JSONObject().apply {
                        put("orderId", orderId)
                        put("amount", amount)
                        put("orderInfo", "Thanh toan don hang JuiceShop")
                    }.toString())
                }
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                withContext(Dispatchers.Main) {
                    if (conn.responseCode == 200 && json.has("payUrl"))
                        onSuccess(json.getString("payUrl"))
                    else
                        onError(json.optString("error", "Không thể tạo đơn MoMo"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Lỗi kết nối: ${e.message}") }
            }
        }
    }

    // Dialog thanh toán

    private fun showPaymentDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_payment)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.BOTTOM)

        val etName          = dialog.findViewById<EditText>(R.id.etCustomerName)
        val etAddress       = dialog.findViewById<EditText>(R.id.etAddress)
        val etPhone         = dialog.findViewById<EditText>(R.id.etPhone)
        val etVoucher       = dialog.findViewById<EditText>(R.id.etVoucher)
        val btnApplyVoucher = dialog.findViewById<TextView>(R.id.btnApplyVoucher)
        val tvSubtotal      = dialog.findViewById<TextView>(R.id.tvSubtotal)
        val tvDiscount      = dialog.findViewById<TextView>(R.id.tvDiscount)
        val tvTotal         = dialog.findViewById<TextView>(R.id.tvTotalAmount)
        val btnConfirm      = dialog.findViewById<Button>(R.id.btnConfirmOrder)
        val btnClose        = dialog.findViewById<ImageButton>(R.id.btnClose)
        val rbCOD           = dialog.findViewById<RadioButton>(R.id.rbCOD)
        val rbOnline        = dialog.findViewById<RadioButton>(R.id.rbOnline)
        val layoutCOD       = dialog.findViewById<LinearLayout>(R.id.layoutCOD)
        val layoutOnline    = dialog.findViewById<LinearLayout>(R.id.layoutOnline)

        val originalTotal   = adapter.getSelectedTotal()  // ← chỉ tính sản phẩm được tick
        discountPercent     = 0.0; maxDiscount = 0.0
        var currentDiscount = 0.0

        rbCOD.isChecked = true; rbOnline.isChecked = false

        fun updateTotals(discountAmount: Double = currentDiscount) {
            currentDiscount = discountAmount
            val finalTotal  = originalTotal - discountAmount
            tvSubtotal.text = "${String.format("%,.0f", originalTotal)} đ"
            tvDiscount.text = "-${String.format("%,.0f", discountAmount)} đ"
            tvTotal.text    = "${String.format("%,.0f", finalTotal)} đ"
        }
        updateTotals(0.0)

        // Load thông tin user
        uid?.let { userId ->
            db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                etName.setText(doc.getString("username") ?: "")
                etPhone.setText(doc.getString("phone") ?: "")
                etAddress.setText(doc.getString("address") ?: "")
            }
        }

        layoutCOD.setOnClickListener    { rbCOD.isChecked = true;    rbOnline.isChecked = false }
        rbCOD.setOnClickListener        { rbCOD.isChecked = true;    rbOnline.isChecked = false }
        layoutOnline.setOnClickListener { rbOnline.isChecked = true; rbCOD.isChecked    = false }
        rbOnline.setOnClickListener     { rbOnline.isChecked = true; rbCOD.isChecked    = false }

        btnApplyVoucher.setOnClickListener {
            val code = etVoucher.text.toString().trim().uppercase()
            if (code.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnApplyVoucher.isEnabled = false; btnApplyVoucher.text = "Đang kiểm tra..."
            applyVoucherFromFirebase(code, originalTotal) { discountAmount, message, success ->
                runOnUiThread {
                    btnApplyVoucher.isEnabled = true; btnApplyVoucher.text = "Áp dụng"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    if (success) updateTotals(discountAmount)
                    else { discountPercent = 0.0; maxDiscount = 0.0; updateTotals(0.0) }
                }
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val name    = etName.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val phone   = etPhone.text.toString().trim()
            if (name.isEmpty())    { Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();        return@setOnClickListener }
            if (phone.isEmpty())   { Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (address.isEmpty()) { Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();       return@setOnClickListener }

            val finalTotal = originalTotal - currentDiscount

            when {
                rbCOD.isChecked -> {
                    viewModel.placeOrder(name, address, phone, finalTotal, adapter.getSelectedItems())
                    // ← CartViewModel.placeOrder đã clearCart cả local lẫn Firestore
                    viewModel.orderState.observe(this) { state ->
                        if (state is CartViewModel.OrderState.Success) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this, "Đặt hàng thành công! 🎉", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            startActivity(Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            })
                        }
                    }
                    dialog.dismiss()
                }
                rbOnline.isChecked -> {
                    btnConfirm.isEnabled = false; btnConfirm.text = "Đang xử lý..."
                    progressBar.visibility = View.VISIBLE
                    viewModel.placeOrder(name, address, phone, finalTotal, adapter.getSelectedItems())
                    viewModel.orderState.observeForever(object : androidx.lifecycle.Observer<CartViewModel.OrderState> {
                        override fun onChanged(state: CartViewModel.OrderState) {
                            if (state is CartViewModel.OrderState.Success) {
                                viewModel.orderState.removeObserver(this)
                                createMomoPayment(state.orderId, finalTotal.toLong(),
                                    onSuccess = { payUrl ->
                                        progressBar.visibility = View.GONE; dialog.dismiss()
                                        momoLauncher.launch(
                                            Intent(this@CartActivity, MomoWebViewActivity::class.java).apply {
                                                putExtra(MomoWebViewActivity.EXTRA_PAY_URL, payUrl)
                                                putExtra(MomoWebViewActivity.EXTRA_ORDER_ID, state.orderId)
                                            }
                                        )
                                    },
                                    onError = { msg ->
                                        progressBar.visibility = View.GONE
                                        btnConfirm.isEnabled = true; btnConfirm.text = "ĐẶT HÀNG"
                                        Toast.makeText(this@CartActivity, "❌ $msg", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else if (state is CartViewModel.OrderState.Error) {
                                viewModel.orderState.removeObserver(this)
                                progressBar.visibility = View.GONE
                                btnConfirm.isEnabled = true; btnConfirm.text = "ĐẶT HÀNG"
                            }
                        }
                    })
                }
            }
        }
        dialog.show()
    }
}