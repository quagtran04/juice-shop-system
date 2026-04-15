package com.example.juiceshop.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.juiceshop.R
import com.example.juiceshop.viewmodel.ChangePasswordViewModel

class ChangePasswordActivity : AppCompatActivity() {

    private val viewModel: ChangePasswordViewModel by viewModels()

    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        bindViews()
        observeViewModel()
        setListeners()
    }

    private fun bindViews() {
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword     = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSave           = findViewById(R.id.btnSave)
        btnBack           = findViewById(R.id.btnBack)
        progressBar       = findViewById(R.id.progressBar)
    }

    private fun setListeners() {
        btnSave.setOnClickListener {
            val current = etCurrentPassword.text.toString().trim()
            val new     = etNewPassword.text.toString().trim()
            val confirm = etConfirmPassword.text.toString().trim()
            viewModel.changePassword(current, new, confirm)
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.changePasswordState.observe(this) { state ->
            when (state) {
                is ChangePasswordViewModel.ChangePasswordState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnSave.isEnabled      = false
                }
                is ChangePasswordViewModel.ChangePasswordState.Success -> {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled      = true
                    Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is ChangePasswordViewModel.ChangePasswordState.Error -> {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled      = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}