package com.example.juiceshop.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.juiceshop.R
import com.example.juiceshop.ui.auth.ChangePasswordActivity
import com.example.juiceshop.ui.auth.LoginActivity
import com.example.juiceshop.ui.cart.CartActivity
import com.example.juiceshop.ui.main.HomeActivity
import com.example.juiceshop.ui.order.OrderListActivity
import com.example.juiceshop.ui.product.SearchActivity
import com.example.juiceshop.viewmodel.ProfileViewModel

class ProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button
    private lateinit var btnChangePassword: TextView
    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        bindViews()
        setupNavigation()
        observeViewModel()
    }

    private fun bindViews() {
        tvUsername       = findViewById(R.id.tvUsername)
        tvEmail          = findViewById(R.id.tvEmail)
        etPhone          = findViewById(R.id.etPhone)
        etAddress        = findViewById(R.id.etAddress)
        btnSave          = findViewById(R.id.btnSave)
        btnLogout        = findViewById(R.id.btnLogout)
        btnChangePassword= findViewById(R.id.tvChangePassword)
        progressBar      = findViewById(R.id.progressBar)
    }

    private fun setupNavigation() {
        btnSave.setOnClickListener {
            val phone   = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()
            viewModel.updateProfile(phone, address)
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.logout()
                    startActivity(
                        Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        // Bottom nav
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
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.user.observe(this) { user ->
            user?.let {
                tvUsername.text = it.username
                tvEmail.text    = it.email
                etPhone.setText(it.phone)
                etAddress.setText(it.address)
            }
        }

        viewModel.updateState.observe(this) { state ->
            when (state) {
                is ProfileViewModel.UpdateState.Success ->
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                is ProfileViewModel.UpdateState.Error ->
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                null -> {}
            }
        }
    }
}