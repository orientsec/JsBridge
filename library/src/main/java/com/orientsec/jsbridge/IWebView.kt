package com.orientsec.jsbridge

import android.content.Context
import android.webkit.ValueCallback

interface IWebView {
    fun getContext(): Context?

    fun loadUrl(url: String?)

    fun addJavascriptInterface(obj: Any?, interfaceName: String?)

    fun evaluateJavascript(var1: String?, callback: ValueCallback<String>?)

    val onPageLoadListener: OnPageLoadListener
}