package com.example.webview_kaswarga

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorContainer: RelativeLayout
    private lateinit var errorTitle: TextView
    private lateinit var errorMessage: TextView
    private lateinit var retryButton: TextView

    private val URL = "http://192.168.130.151:8080/login/"

    // Variables untuk file upload
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val REQUEST_SELECT_FILE = 100

    companion object {
        private const val TAG = "MainActivity"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupWebView()

        if (isInternetAvailable()) {
            loadWebView()
        } else {
            showErrorView()
        }
    }

    private fun initializeViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorContainer = findViewById(R.id.errorContainer)
        errorTitle = findViewById(R.id.errorTitle)
        errorMessage = findViewById(R.id.errorMessage)
        retryButton = findViewById(R.id.retryButton)

        retryButton.setOnClickListener {
            if (isInternetAvailable()) {
                hideErrorView()
                loadWebView()
            }
        }
    }

    private fun loadWebView() {
        webView.visibility = android.view.View.VISIBLE
        webView.loadUrl(URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = webView.settings

        // Enable basic settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccess = true
        webSettings.setSupportMultipleWindows(true)

        // Important for file upload
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true

        // Set WebView Client
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = android.view.View.VISIBLE
                hideErrorView()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = android.view.View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                progressBar.visibility = android.view.View.GONE
                if (!isInternetAvailable()) {
                    showErrorView()
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: android.webkit.SslErrorHandler,
                error: android.net.http.SslError
            ) {
                handler.proceed()
            }
        }

        // Set WebChrome Client untuk file upload
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress == 100) android.view.View.GONE else android.view.View.VISIBLE
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                Log.d(TAG, "onShowFileChooser called")

                // Cancel previous callback
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                showModernFilePicker()
                return true
            }
        }
    }

    private fun showModernFilePicker() {
        try {
            Log.d(TAG, "Showing modern file picker")

            // Gunakan ACTION_OPEN_DOCUMENT untuk dialog modern dari bawah
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                        "image/*",
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    ))
                    // Flag untuk persistable permission
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                // Fallback untuk Android lama
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
            }

            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }

            // Buat chooser dengan opsi
            val chooserIntent = Intent.createChooser(intent, "Pilih File").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(galleryIntent))
            }

            startActivityForResult(chooserIntent, REQUEST_SELECT_FILE)

        } catch (e: Exception) {
            Log.e(TAG, "Error showing file picker: ${e.message}")
            // Fallback ke method sederhana
            showSimpleFilePicker()
        }
    }

    private fun showSimpleFilePicker() {
        try {
            // Fallback method yang lebih sederhana
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }

            startActivityForResult(Intent.createChooser(intent, "Pilih File"), REQUEST_SELECT_FILE)
        } catch (e: Exception) {
            Log.e(TAG, "Error in fallback file picker: ${e.message}")
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == REQUEST_SELECT_FILE) {
            if (filePathCallback == null) {
                Log.w(TAG, "filePathCallback is null")
                return
            }

            var results: Array<Uri>? = null

            if (resultCode == Activity.RESULT_OK && data != null) {
                val uri = data.data
                if (uri != null) {
                    Log.d(TAG, "File selected: $uri")

                    // Take persistable permission untuk file yang dipilih
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot take persistable permission: ${e.message}")
                    }

                    results = arrayOf(uri)
                }
            }

            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }

    private fun showErrorView() {
        webView.visibility = android.view.View.GONE
        errorContainer.visibility = android.view.View.VISIBLE
        progressBar.visibility = android.view.View.GONE

        errorTitle.text = "Tidak Ada Koneksi Internet"
        errorMessage.text = "Pastikan Wi-Fi atau data seluler Anda aktif, lalu coba lagi."
        retryButton.text = "Coba Lagi"
    }

    private fun hideErrorView() {
        errorContainer.visibility = android.view.View.GONE
        webView.visibility = android.view.View.VISIBLE
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null && (
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}