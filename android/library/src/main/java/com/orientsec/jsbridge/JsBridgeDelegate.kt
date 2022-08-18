package com.orientsec.jsbridge

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.webkit.JavascriptInterface
import androidx.annotation.MainThread
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class JsBridgeDelegate(private val webView: BridgeWebView) : JsBridge, PageListener,
    Loggable by BridgeLogger {

    /**
     * Native callback id. Auto increament.
     */
    private val mUniqueId = AtomicLong(0)

    /**
     * Native handlers for js.
     */
    private val mMessageHandlers: MutableMap<String, BridgeHandler> =
        ConcurrentHashMap<String, BridgeHandler>()

    /**
     * Native callbacks to receive js result.
     */
    private val mCallbacks: MutableMap<String, BridgeCallback> = ConcurrentHashMap()

    /**
     * Cached native request before page loaded.
     */
    private var mRequests: MutableList<String>? = ArrayList()

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlerName handlerName
     * @param handler     BridgeHandler
     */
    @MainThread
    override fun registerHandler(handlerName: String, handler: BridgeHandler) {
        // 添加至 Map<String, BridgeHandler>
        mMessageHandlers[handlerName] = handler
    }


    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlers handlerName
     */
    @MainThread
    override fun registerHandler(handlers: Map<String, BridgeHandler>) {
        mMessageHandlers.putAll(handlers)
    }


    /**
     * unregister handler
     *
     * @param handlerName
     */
    @MainThread
    override fun unregisterHandler(handlerName: String) {
        mMessageHandlers.remove(handlerName)
    }


    @JavascriptInterface
    fun request(handlerName: String, data: String, callbackId: String?) {
        info("requestFromJs->$handlerName, $data, callbackId: $callbackId")
        val callback = callbackOfId(callbackId)
        val handler = mMessageHandlers[handlerName]
        if (handler == null) {
            warn("No handler for [$handlerName].")
            callback.onError(-1, "Handler for [$handlerName] not found.")
        } else {
            handler.handle(data, callback)
        }
    }

    private fun callbackOfId(callbackId: String?): BridgeCallback {
        return if (callbackId.isNullOrEmpty()) EmptyBridgeCallback
        else DispatchCallback(callbackId, this)
    }

    @JavascriptInterface
    fun response(code: Int, info: String, data: String, callbackId: String) {
        info("responseFromJs->code[$code], info[$info], data[$data], callbackId[$callbackId]")
        mCallbacks.remove(callbackId)?.apply {
            if (code == 0) onResult(data)
            else onError(code, info)
        }
    }

    /**
     * 调用Js handler。
     *
     * @param handlerName      HandlerName.
     * @param data             Request data.
     * @param responseCallback BridgeCallback.
     */
    @MainThread
    override fun callHandler(
        handlerName: String,
        data: String,
        responseCallback: BridgeCallback?
    ) {
        var callbackId = ""
        if (responseCallback != null) {
            callbackId = callbackId(mUniqueId.incrementAndGet())
            mCallbacks[callbackId] = responseCallback
        }
        val requestScript = requestScript(handlerName, data, callbackId)
        val requests = mRequests
        if (requests != null) {
            //等待状态添加到消息集合否则分发消息
            requests.add(requestScript)
        } else {
            callJs(requestScript)
        }
    }

    internal fun callJs(script: String) {
        runOnUiThread {
            info("callJs->$script")
            webView.evaluateJavascript(script, null)
        }
    }

    private fun callJs(scripts: List<String>) {
        runOnUiThread {
            scripts.forEach {
                info("callJs->$it")
                webView.evaluateJavascript(it, null)
            }
        }
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

    override fun onLoadStart() {
    }

    override fun onLoadFinished() {
        val requests = mRequests
        if (requests != null) {
            callJs(requests)
            mRequests = null
        }
    }

    private fun requestScript(handlerName: String, data: String, callbackId: String): String {
        val formattedData = data.replace("\\", "\\\\")
        return String.format(JS_REQUEST_FROM_NATIVE, handlerName, formattedData, callbackId)
    }

    private fun callbackId(uniqueId: Long): String {
        return String.format(
            CALLBACK_ID_FORMAT,
            uniqueId,
            SystemClock.currentThreadTimeMillis()
        )
    }

    companion object {
        private const val CALLBACK_ID_FORMAT = "JAVA_CB_%s_%s"
        private const val JS_REQUEST_FROM_NATIVE =
            "javascript:jsBridge.request('%s','%s','%s');"
        internal const val JS_RESPONSE_FROM_NATIVE =
            "javascript:jsBridge.response('%d','%s','%s','%s');"
    }
}

internal class DispatchCallback(
    private val callbackId: String,
    private val jsBridgeDelegate: JsBridgeDelegate
) : BridgeCallback {
    override fun onResult(data: String) {
        jsBridgeDelegate.callJs(responseScript(0, "OK", data, callbackId))
    }

    override fun onError(code: Int, info: String) {
        jsBridgeDelegate.callJs(responseScript(code, info, "", callbackId))
    }

    private fun responseScript(
        code: Int,
        info: String,
        data: String,
        callbackId: String
    ): String {
        val formattedData = data.replace("\\", "\\\\")
        return String.format(
            JsBridgeDelegate.JS_RESPONSE_FROM_NATIVE,
            code,
            info,
            formattedData,
            callbackId
        )
    }
}