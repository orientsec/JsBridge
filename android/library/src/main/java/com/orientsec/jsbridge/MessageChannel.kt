package com.orientsec.jsbridge

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

interface MessageChannel {

    fun postMessage(message: String)

    fun addMessageListener(messageListener: MessageListener)

    fun removeMessageListener(messageListener: MessageListener)
}

fun interface MessageListener {
    fun onMessage(message: String)
}

class SafeMessageChannel(webView: WebView) : MessageChannel,
    WebViewCompat.WebMessageListener, Loggable by BridgeLogger {
    private var javaScriptReplyProxy: JavaScriptReplyProxy? = null
    private val listeners: MutableSet<MessageListener> = mutableSetOf()

    /**
     * Cached native request before page loaded.
     */
    private val messageList = mutableListOf<String>()

    init {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(
                webView,
                "bridgePort",
                setOf("*"),
                this
            )
        } else {
            error("Not support WEB_MESSAGE_LISTENER.")
        }
    }

    override fun postMessage(message: String) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            val replyProxy = javaScriptReplyProxy
            if (replyProxy == null) {
                messageList.add(message)
            } else {
                replyProxy.postMessage(message)
            }
        } else {
            error("Not support WEB_MESSAGE_LISTENER.")
        }
    }

    private fun postMessageOnInit(replyProxy: JavaScriptReplyProxy) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            messageList.forEach {
                replyProxy.postMessage(it)
            }
            messageList.clear()
        } else {
            error("Not support WEB_MESSAGE_LISTENER.")
        }
    }

    override fun addMessageListener(messageListener: MessageListener) {
        listeners.add(messageListener)
    }

    override fun removeMessageListener(messageListener: MessageListener) {
        listeners.remove(messageListener)
    }

    @UiThread
    override fun onPostMessage(
        view: WebView,
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy
    ) {
        javaScriptReplyProxy = replyProxy
        val data = message.data
        if (data.isNullOrEmpty()) {
            warn("receive empty message from h5.")
            return
        } else if (data.startsWith("JsBridge-Channel-Init")) {
            postMessageOnInit(replyProxy)
        } else {
            listeners.forEach { it.onMessage(data) }
        }
    }
}

internal const val JS_MESSAGE_FROM_NATIVE =
    "javascript:jsBridge.onMessage('%s');"

class UnsafeMessageChannel(private val webView: WebView) : MessageChannel,
    Loggable by BridgeLogger {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners: MutableSet<MessageListener> = mutableSetOf()

    /**
     * Cached native request before page loaded.
     */
    private var messageList: MutableList<String>? = mutableListOf()

    init {
        webView.addJavascriptInterface(this, "bridgeChannel")
    }

    override fun postMessage(message: String) {
        val messageList = messageList
        if (messageList == null) {
            val script = String.format(JS_MESSAGE_FROM_NATIVE, message.replace("\\", "\\\\"))
            runOnUiThread {
                info("callJs->$script")
                webView.evaluateJavascript(script, null)
            }
        } else {
            messageList.add(message)
        }
    }

    private fun postMessageOnInit() {
        val messageList = messageList ?: return
        runOnUiThread {
            messageList.forEach {
                val script = String.format(JS_MESSAGE_FROM_NATIVE, it)
                info("callJs->$script")
                webView.evaluateJavascript(script, null)
            }
        }
        this.messageList = null
    }

    private fun runOnUiThread(runnable: () -> Unit) {
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            runnable()
        } else {
            mainHandler.post {
                runnable()
            }
        }
    }


    override fun addMessageListener(messageListener: MessageListener) {
        listeners.add(messageListener)
    }

    override fun removeMessageListener(messageListener: MessageListener) {
        listeners.remove(messageListener)
    }

    @JavascriptInterface
    fun onMessage(message: String) {
        if (message.startsWith("JsBridge-Channel-Init")) {
            postMessageOnInit()
        } else {
            listeners.forEach { it.onMessage(message) }
        }
    }
}
