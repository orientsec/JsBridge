package com.orientsec.jsbridge

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.system.measureTimeMillis


class BridgeWebView : WebView, JsBridge {
    private val jsBridge: JsBridgeDelegate = JsBridgeDelegate(this)
    private val bridgeWebViewClient: BridgeWebViewClient = BridgeWebViewClient()

    constructor(context: Context) : super(context.fixedContext())
    constructor(context: Context, attrs: AttributeSet?) : super(context.fixedContext(), attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.fixedContext(),
        attrs,
        defStyleAttr
    )

    init {
        super.setWebViewClient(bridgeWebViewClient)
        addJavascriptInterface(jsBridge, "nativeBridge")
        if (debug) {
            setWebContentsDebuggingEnabled(true)
        }
    }

    override fun setWebViewClient(webViewClient: WebViewClient) {
        bridgeWebViewClient.webViewClient = webViewClient
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
        responseCallback: BridgeCallback?
    ) {
        jsBridge.callHandler(handlerName, data, responseCallback)
    }


    /**
     * 这里只是加载lib包中assets中的 index.min.js。
     */
    internal fun loadJs() {
        jsBridge.onLoadStart()
        val mill = measureTimeMillis {
            try {
                val js = context.assets.open("jsbridge/index.min.js")
                    .bufferedReader()
                    .use {
                        val sb = StringBuilder()
                        do {
                            val line = it.readLine()
                            sb.append(line)
                        } while (line != null)
                        sb.toString()
                    }
                loadUrl("javascript:$js")
            } catch (e: Exception) {
                BridgeLogger.error("Js bridge script load failed.", e)
            }

        }
        BridgeLogger.info("load js bridge script in:$mill ms")
        jsBridge.onLoadFinished()
    }
}