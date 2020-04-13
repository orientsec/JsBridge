package com.orientsec.jsbridge

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import android.webkit.WebViewClient

@SuppressLint("SetJavaScriptEnabled")
class BridgeWebView : WebView, IWebView {

    val jsBridge: JsBridge = JsBridge(this)
    private var bridgeWebViewClient: BridgeWebViewClient = BridgeWebViewClient(jsBridge)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        super.setWebViewClient(bridgeWebViewClient)
        addJavascriptInterface(jsBridge, "jsBridge")
        if (Logger.debug) {
            setWebContentsDebuggingEnabled(true)
        }
    }

    override fun setWebViewClient(webViewClient: WebViewClient) {
        bridgeWebViewClient.webViewClient = webViewClient
    }

    override fun destroy() {
        super.destroy()
        jsBridge.clean()
        context
    }

}