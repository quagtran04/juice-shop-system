package com.example.juiceshop.data

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.juiceshop.BuildConfig

object CloudinaryManager {

    fun init(context: Context) {
        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME
        )
        MediaManager.init(context, config)
    }

    fun uploadImage(
        imageUri: Uri,
        folder: String,               // "products" | "categories" | "users"
        onSuccess: (url: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        MediaManager.get()
            .upload(imageUri)
            .option("folder", folder)
            .option("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String ?: ""
                    onSuccess(url)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description)
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
}