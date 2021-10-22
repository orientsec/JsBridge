package com.orientsec.jsbridge

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebView
import android.webkit.WebViewClient


@SuppressLint("SetJavaScriptEnabled")
class BridgeWebView : WebView, IWebView, IJSBridge, OnPageLoadListener {
    private var mOnPageLoadListener: OnPageLoadListener = this
    private val jsBridge: JsBridge = JsBridge(this)
    private val bridgeWebViewClient: BridgeWebViewClient = BridgeWebViewClient(jsBridge)

    constructor(context: Context) : super(getFixedContext(context))
    constructor(context: Context, attrs: AttributeSet?) : super(getFixedContext(context), attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        getFixedContext(context),
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

    companion object {
        private fun getFixedContext(context: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
            ) {
                return context.createConfigurationContext(Configuration())
            }
            return context
        }
    }
}