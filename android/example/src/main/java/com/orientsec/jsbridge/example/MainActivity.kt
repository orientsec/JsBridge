package com.orientsec.jsbridge.example

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
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
        debug = true
        webView = findViewById(R.id.webView)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener(this)
        webView.loadUrl("http:///192.168.106.129:8080")
        webView.registerHandler("hello") { data, callback ->
            Log.i("MainActivity", "handler = hello, data from web = $data")
            callback.onResult("Hello! Welcome to visit native!")
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                return super.onJsAlert(view, url, message, result)
            }
        }
    }

    override fun onClick(v: View) {
        if (R.id.button == v.id) {
            webView.callHandler(
                "hello",
                "hello \\ from \\Java \\kotlin",
                object : BridgeCallback {
                    override fun onResult(data: String) {
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
}