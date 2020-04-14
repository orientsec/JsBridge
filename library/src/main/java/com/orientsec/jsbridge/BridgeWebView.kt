package com.orientsec.jsbridge

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import android.webkit.WebViewClient

@SuppressLint("SetJavaScriptEnabled")
class BridgeWebView : WebView, IWebView, IJSBridge, OnPageLoadListener {
    var mOnPageLoadListener: OnPageLoadListener = this
    private val jsBridge: JsBridge = JsBridge(this)
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
        if (JsBridge.debug) {
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

    override fun registerHandler(handlerName: String, handler: BridgeHandler) {
        jsBridge.registerHandler(handlerName, handler)
    }

    override fun registerHandler(handlers: Map<String, BridgeHandler>) {
        jsBridge.registerHandler(handlers)
    }

    override fun unregisterHandler(handlerName: String) {
        jsBridge.unregisterHandler(handlerName)
    }

    override fun callHandler(
        handlerName: String,
        data: String,
        responseCallback: ((String) -> Unit)?
    ) {
        jsBridge.callHandler(handlerName, data, responseCallback)
    }

    override fun onPageLoaded(isLoaded: Boolean) {

    }

    fun setOnPageLoadListener(onPageLoadListener: OnPageLoadListener) {
        this.mOnPageLoadListener = onPageLoadListener
    }

    override val onPageLoadListener: OnPageLoadListener
        get() = mOnPageLoadListener
}