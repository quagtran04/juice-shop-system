package com.example.juiceshop.ui.cart

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.juiceshop.R

class MomoWebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAY_URL  = "pay_url"
        const val EXTRA_ORDER_ID = "order_id"
        const val RESULT_SUCCESS = 1001
        const val RESULT_FAILED  = 1002
        private const val CALLBACK_URL = "https://milled-lethally-pearline.ngrok-free.dev/api/momo/callback"
        private const val TAG = "MomoWebView"
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_momo_webview)

        val payUrl  = intent.getStringExtra(EXTRA_PAY_URL)  ?: run { finish(); return }
        val orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""

        Log.d(TAG, "Loading payUrl: $payUrl")

        webView     = findViewById(R.id.webViewMomo)
        progressBar = findViewById(R.id.progressBarMomo)

        webView.settings.apply {
            javaScriptEnabled    = true
            domStorageEnabled    = true
            loadWithOverviewMode = true
            useWideViewPort      = true
            mixedContentMode     = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(false)
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                Log.d(TAG, "Navigating to: $url")

                // ✅ MoMo callback → thanh toán xong
                if (url.startsWith(CALLBACK_URL)) {
                    val resultCode = request.url.getQueryParameter("resultCode")
                    Log.d(TAG, "Callback resultCode: $resultCode")
                    setResult(
                        if (resultCode == "0") RESULT_SUCCESS else RESULT_FAILED,
                        Intent().putExtra(EXTRA_ORDER_ID, orderId)
                    )
                    finish()
                    return true
                }

                // ✅ Deep link momo:// → mở app MoMo thật
                if (url.startsWith("momo://")) {
                    Log.d(TAG, "Opening MoMo app via deeplink: $url")
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "MoMo app not installed: ${e.message}")
                        Toast.makeText(
                            this@MomoWebViewActivity,
                            "Vui lòng cài app MoMo để thanh toán!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return true
                }

                // ✅ Các scheme lạ khác (intent://, market://, ...) → bỏ qua
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    Log.w(TAG, "Ignoring unknown scheme: $url")
                    return true
                }

                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = View.GONE
                Log.d(TAG, "Page finished: $url")
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                Log.e(TAG, "Error on ${request.url}: ${error.errorCode} - ${error.description}")
                if (request.isForMainFrame) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@MomoWebViewActivity,
                        "Lỗi tải trang: ${error.description}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        webView.loadUrl(payUrl)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            setResult(RESULT_FAILED)
            super.onBackPressed()
        }
    }
}