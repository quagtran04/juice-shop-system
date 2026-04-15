package com.example.juiceshop.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.juiceshop.R
import com.example.juiceshop.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Handler(Looper.getMainLooper()).postDelayed({
            // Nếu đã đăng nhập thì vào thẳng Home, ngược lại vào Login
            val destination = if (FirebaseAuth.getInstance().currentUser != null)
                HomeActivity::class.java
            else
                LoginActivity::class.java

            startActivity(Intent(this, destination))
            finish()
        }, 2000) // Splash 2 giây
    }
}