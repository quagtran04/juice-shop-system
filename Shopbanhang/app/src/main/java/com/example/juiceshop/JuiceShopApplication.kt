package com.example.juiceshop

import android.app.Application
import com.example.juiceshop.data.CloudinaryManager

class JuiceShopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CloudinaryManager.init(this)
    }
}