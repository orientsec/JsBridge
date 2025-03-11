package com.orientsec.jsbridge

import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/**
 * Interface MessageChannel defines the behavior and management mechanisms for message passing
 * between Native and JavaScript.
 */
interface MessageChannel {
    /**
     * Called when the channel becomes active.
     * This can be used to perform initialization or preparation operations to ensure the
     * channel is ready for use.
     */
    fun active()

    /**
     * Sends a message through the channel.
     *
     * @param message The content of the message to be sent.
     */
    fun postMessage(message: String)

    /**
     * Adds a message listener to handle received messages.
     *
     * @param messageListener The instance of the message listener to be added.
     */
    fun addMessageListener(messageListener: MessageListener)

    /**
     * Removes a message listener from the channel.
     *
     * @param messageListener The instance of the message listener to be removed.
     */
    fun removeMessageListener(messageListener: MessageListener)
}

/**
 * Functional interface MessageListener is used to listen and handle message events.
 * It defines a single abstract method onMessage, which is called when a new message arrives.
 * The primary purpose is to set up listeners where message events need to be handled
 * asynchronously.
 */
fun interface MessageListener {
    /**
     * Called when a new message arrives. This method is responsible for handling the incoming
     * message string.
     *
     * @param message The content of the message, a string containing the received message.
     */
    fun onMessage(message: String)
}

/**
 * Class WebkitMessageChannel implements the MessageChannel interface.
 * It receives messages through WebViewCompat.WebMessageListener and sends messages through
 * JavaScriptReplyProxy.
 * With the support of the androidx.webkit component, it ensures safer interaction with WebView.
 *
 * @param webView The WebView instance used for communication, through which messages can be sent
 * and received.
 */
class WebkitMessageChannel(private val webView: WebView) : MessageChannel,
    WebViewCompat.WebMessageListener, Loggable by BridgeLogger {

    private var javaScriptReplyProxy: JavaScriptReplyProxy? = null
    private val listeners: MutableSet<MessageListener> = mutableSetOf()

    override fun active() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(
                webView,
                "bridgePort",
                setOf("*"),
                this
            )
        } else {
            error("not supported: WEB_MESSAGE_LISTENER")
        }
    }

    override fun postMessage(message: String) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            val replyProxy = javaScriptReplyProxy
            if (replyProxy == null) {
                error(
                    "postMessage should be called after the channel receives an initialized " +
                            "message from web"
                )
            } else {
                info("replyProxy postMessage -> $message")
                replyProxy.postMessage(message)
            }
        } else {
            error("not supported: WEB_MESSAGE_LISTENER")
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
            warn("received empty message from web")
        } else {
            listeners.forEach { it.onMessage(data) }
        }
    }
}

/**
 * Class StandardMessageChannel implements the MessageChannel interface to support message passing
 * between Native and JavaScript.
 *
 * @param webView An instance of [IBridgeWebView], used as a bridge for message passing.
 */
class StandardMessageChannel(private val webView: IBridgeWebView) : MessageChannel,
    Loggable by BridgeLogger {

    companion object {
        private const val JS_MESSAGE_FROM_NATIVE = "javascript:jsBridge.onMessage('%s');"
    }

    private val listeners: MutableSet<MessageListener> = mutableSetOf()

    override fun active() {
        webView.addJavascriptInterface(this, "bridgeChannel")
    }

    override fun postMessage(message: String) {
        // Escape special characters for JSON string
        val formattedMessage = JSONObject.quote(message)
        val script = String.format(JS_MESSAGE_FROM_NATIVE, formattedMessage)
        // Must find the main thread to pass data out IMPORTANT
        webView.runOnUiThread {
            info("evaluate javascript -> $script")
            webView.evaluateJavascript(script, null)
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
        if (message.isEmpty()) {
            warn("received empty message from web")
        } else {
            listeners.forEach { webView.runOnUiThread { it.onMessage(message) } }
        }
    }
}
