/*
 * MIT License
 *
 * Copyright (c) 2022 JD.com, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.orientsec.jsbridge

import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebViewClient
import kotlin.system.measureTimeMillis
import android.webkit.WebView
import androidx.webkit.WebViewFeature

/**
 * Interface for a WebView that supports JavaScript bridging.
 *
 * This interface extends JsBridge and provides additional methods or properties
 * specific to a WebView that can communicate with JavaScript.
 */
interface IBridgeWebView : JsBridge, PageOnLoadListener {
    val view: View

    val jsBridge: JsBridge

    fun getUrl(): String?

    fun addJavascriptInterface(obj: Any, interfaceName: String)

    fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>? = null)

    fun loadUrl(url: String)

    fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>)

    fun reload()

    override fun registerHandler(name: String, handler: BridgeHandler) {
        jsBridge.registerHandler(name, handler)
    }

    override fun registerHandler(handlers: Map<String, BridgeHandler>) {
        jsBridge.registerHandler(handlers)
    }

    override fun unregisterHandler(name: String) {
        jsBridge.unregisterHandler(name)
    }

    override fun callHandler(name: String, data: String, responseCallback: BridgeCallback?) {
        jsBridge.callHandler(name, data, responseCallback)
    }

}

/**
 * Install a JavaScript bridge in the given WebView.
 *
 * This function installs a JavaScript bridge in the provided IBridgeWebView instance,
 * enabling communication between JavaScript and native code.
 * It returns a JsBridge instance that can be used to register handlers for JavaScript requests.
 *
 * installed.
 * @return A JsBridge instance for registering handlers to process JavaScript requests.
 */
fun IBridgeWebView.install(): JsBridge {
    val webView = view as? WebView
    val channel = if (webView != null &&
        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
    ) {
        WebkitMessageChannel(webView)
    } else {
        StandardMessageChannel(this)
    }
    return JsBridgeInternal(channel, this).apply { install() }
}

/**
 * Installs a JavaScript bridge using the given message channel and web view.
 *
 * This function creates an instance of [JsBridgeInternal] with the provided [MessageChannel]
 * and [IBridgeWebView], and installs the bridge.
 * It returns the [JsBridge] instance that can be used to handle JavaScript requests.
 *
 * @param channel The [MessageChannel] to be used for communication between JavaScript and
 * Native code.
 * @return A [JsBridge] instance for handling JavaScript requests.
 */
fun IBridgeWebView.install(channel: MessageChannel): JsBridge {
    return JsBridgeInternal(channel, this).apply { install() }
}

/**
 * Loads the JsBridge JavaScript script into the H5 page.
 *
 * This method must be called manually when using JsBridge to establish the communication channel.
 * It should be called every time the page is reloaded or the URL changes to ensure the JavaScript
 * script is reloaded into the page.
 * It is recommended to call this method in [WebViewClient.onPageFinished] to ensure the script is
 * loaded after the page has finished loading. Calling it before the page is fully loaded will have
 * no effect.
 * For more details, refer to [WebView.evaluateJavascript].
 *
 * It will also trigger [PageOnLoadListener.onStart].
 *
 * @see WebViewClient.onPageFinished
 * @see WebView.evaluateJavascript
 */
fun IBridgeWebView.loadJsBridgeScript() {
    val mill = measureTimeMillis {
        try {
            val js = view.context.assets.open("jsbridge/index.min.js")
                .bufferedReader()
                .use {
                    val sb = StringBuilder()
                    do {
                        val line = it.readLine()
                        sb.append(line)
                    } while (line != null)
                    sb.toString()
                }
            evaluateJavascript("javascript:$js")
        } catch (e: Exception) {
            BridgeLogger.error("Js bridge script load failed.", e)
        }
    }
    BridgeLogger.info("Load js bridge script in: $mill ms")
    onStart()
}

/**
 * Resets the request queue.
 *
 * This function aims to reset the request queue for JS interactions, ensuring it can be
 * cleared and restarted under specific conditions.
 */
internal fun IBridgeWebView.resetRequestQueue() {
    (jsBridge as? JsBridgeInternal)?.resetRequestQueue()
}

/**
 * Destroys the current BridgeWebView instance and releases related resources.
 */
internal fun IBridgeWebView.destroy() {
    (jsBridge as? JsBridgeInternal)?.destroy()
}
