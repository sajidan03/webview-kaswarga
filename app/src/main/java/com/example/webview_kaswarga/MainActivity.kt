package com.example.webview_kaswarga

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private val URL = "http://192.168.130.143:8080/login/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        // Configure WebView
        setupWebView()

        // Load the URL
        webView.loadUrl(URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = webView.settings

        // Enable JavaScript
        webSettings.javaScriptEnabled = true

        // Enable DOM storage
        webSettings.domStorageEnabled = true

        // Enable zoom controls
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        // Enable responsive layout
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        // Enable other useful settings
        webSettings.allowContentAccess = true
        webSettings.allowFileAccess = true
        webSettings.setSupportMultipleWindows(true)

        // Set WebView Client
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            // Handle SSL errors (jika website menggunakan SSL)
            override fun onReceivedSslError(
                view: WebView?,
                handler: android.webkit.SslErrorHandler,
                error: android.net.http.SslError
            ) {
                // Untuk development, bisa bypass SSL error
                handler.proceed()
            }
        }

        // Set WebChrome Client untuk progress
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress

                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    // Handle back button to navigate web history
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}