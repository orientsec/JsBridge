package com.xiaomao.jsbridge

import android.webkit.ValueCallback

interface IWebView {

    fun loadUrl(url: String?)

    fun addJavascriptInterface(obj: Any?, interfaceName: String?)

    fun evaluateJavascript(var1: String?, callback: ValueCallback<String>?)

}