package com.orientsec.jsbridge.example

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import com.orientsec.jsbridge.*

class MainActivity : Activity(), View.OnClickListener {
    private lateinit var webView: BridgeWebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WebView.setWebContentsDebuggingEnabled(true)
        JsBridge.debug = true
        webView = findViewById(R.id.webView)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        val button = findViewById<Button>(R.id.btn_call_js)
        button.setOnClickListener(this)
        webView.loadUrl("file:///android_asset/index.html")

        webView.registerHandler("hello") { data, callback ->
            Log.i("MainActivity", "handler = hello, data = $data")
            callback.onSuccess("Hello! Welcome to visit native!")
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.loadJsBridgeScript()
            }
        }
    }

    override fun onClick(v: View) {
        webView.callHandler(
            "hello",
            "hello \\ from \\Java \\kotlin",
            object : BridgeCallback {
                override fun onSuccess(data: String?) {
                    Toast.makeText(
                        this@MainActivity,
                        "Response from js: $data",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onError(code: Int, info: String) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error from js: $code, $info",
                        Toast.LENGTH_LONG
                    ).show()

                }
            })
    }
}