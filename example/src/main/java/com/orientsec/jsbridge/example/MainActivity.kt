package com.orientsec.jsbridge.example

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.gson.Gson
import com.orientsec.jsbridge.BridgeHandler
import com.orientsec.jsbridge.BridgeWebView
import com.orientsec.jsbridge.Logger

class MainActivity : Activity(), View.OnClickListener {
    private lateinit var webView: BridgeWebView

    data class Location(
        val address: String
    )

    data class User(
        val name: String,
        val location: Location,
        val testStr: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Logger.debug = true
        webView = findViewById(R.id.webView)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener(this)
        webView.loadUrl("file:///android_asset/demo.html")
        webView.jsBridge.registerHandler("submitFromWeb", object : BridgeHandler {
            override fun handle(data: String, callback: (String) -> Unit) {
                Log.i("MainActivity", "handler = submitFromWeb, data from web = $data")
                callback("submitFromWeb exe, response data 中文 from Java")
            }
        })
        val location =
            Location("SDU")
        val user = User("大头鬼", location, "")

        webView.jsBridge.callHandler("functionInJs", Gson().toJson(user)) {}

    }

    override fun onClick(v: View) {
        if (R.id.button == v.id) {
            webView.jsBridge.callHandler(
                "functionInJs",
                "data from Java"
            ) {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
}