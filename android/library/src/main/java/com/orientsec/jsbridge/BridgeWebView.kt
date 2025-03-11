package com.orientsec.jsbridge

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView


class BridgeWebView : WebView, IBridgeWebView {
    override val view: View = this
    override val jsBridge: JsBridge = install()

    constructor(context: Context) : super(context.fixedContext())
    constructor(context: Context, attrs: AttributeSet?) : super(context.fixedContext(), attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.fixedContext(),
        attrs,
        defStyleAttr
    )

    init {
        if (JsBridge.debug) {
            setWebContentsDebuggingEnabled(true)
        }
    }

    override fun loadUrl(url: String) {
        super.loadUrl(url)
        if (url.startsWith("http") || url.startsWith("file")) {
            resetRequestQueue()
        }
    }

    override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {
        super.loadUrl(url, additionalHttpHeaders)
        resetRequestQueue()
    }

    override fun reload() {
        super.reload()
        resetRequestQueue()
    }

    override fun destroy() {
        super.destroy()
        (this as IBridgeWebView).destroy()
    }
}