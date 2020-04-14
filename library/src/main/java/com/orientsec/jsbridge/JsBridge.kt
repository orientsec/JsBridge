package com.orientsec.jsbridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.webkit.JavascriptInterface
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

class JsBridge(private val webView: IWebView) {

    private val mUniqueId = AtomicLong(0)

    private val mMessageHandlers: MutableMap<String, BridgeHandler> = mutableMapOf()

    private val mCallbacks: MutableMap<String, (String) -> Unit> = mutableMapOf()

    private var mRequests: MutableList<JSRequest>? = ArrayList()

    private val mainHandler = Handler(Looper.getMainLooper())

    var onPageLoadListener: OnPageLoadListener? = null

    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlerName handlerName
     * @param handler     BridgeHandler
     */
    fun registerHandler(handlerName: String, handler: BridgeHandler) {
        // 添加至 Map<String, BridgeHandler>
        mMessageHandlers[handlerName] = handler
    }


    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlers handlerName
     */
    fun registerHandler(handlers: Map<String, BridgeHandler>) {
        mMessageHandlers.putAll(handlers)
    }


    /**
     * unregister handler
     *
     * @param handlerName
     */
    fun unregisterHandler(handlerName: String) {
        mMessageHandlers.remove(handlerName)
    }


    @JavascriptInterface
    fun callHandler(handlerName: String, data: String, callbackId: String?) {
        val handler = mMessageHandlers[handlerName] ?: return
        if (callbackId == null) {
            handler.handle(data)
        } else {
            handler.handle(data) {
                responseToJs(it, callbackId)
            }
        }
    }

    @JavascriptInterface
    fun response(data: String, callbackId: String) {
        Logger.d(
            javaClass.simpleName,
            "response->$data, callbackId: $callbackId ${Thread.currentThread().name}"
        )
        mCallbacks.remove(callbackId)?.invoke(data)
    }

    fun register(view: IWebView) {
        registerHandler("onPageLoad", object : BridgeHandler {
            override fun handle(data: String, callback: (String) -> Unit) {
                if (Thread.currentThread() === Looper.getMainLooper().thread) {
                    onPageLoadListener?.onPageLoaded(data.isNotEmpty())
                } else {
                    mainHandler.post { onPageLoadListener?.onPageLoaded(data.isNotEmpty()) }
                }
            }
        })
        loadJs(view)
        val requests = mRequests ?: return
        for (request in requests) {
            dispatchMessage(request.handlerName, request.data, request.callbackId)
        }
        mRequests = null
    }

    /**
     * free memory
     */
    fun clean() {
        mCallbacks.clear()
        mMessageHandlers.clear()
        mRequests?.clear()
    }

    private fun responseToJs(data: String, callbackId: String) {
        var messageJson = data
        //escape special characters for json string  为json字符串转义特殊字符
        messageJson = messageJson.replace("(\\\\)([^utrn])".toRegex(), "\\\\\\\\$1$2")
        messageJson = messageJson.replace("(?<=[^\\\\])(\")".toRegex(), "\\\\\"")
        messageJson = messageJson.replace("(?<=[^\\\\])(\')".toRegex(), "\\\\\'")
        messageJson = messageJson.replace("%7B".toRegex(), URLEncoder.encode("%7B"))
        messageJson = messageJson.replace("%7D".toRegex(), URLEncoder.encode("%7D"))
        messageJson = messageJson.replace("%22".toRegex(), URLEncoder.encode("%22"))
        val javascriptCommand =
            String.format(
                BridgeUtil.JS_RESPONSE_FROM_JAVA,
                messageJson,
                callbackId
            )
        Logger.d(javaClass.simpleName, "javascriptCommand->$javascriptCommand")

        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            webView.evaluateJavascript(javascriptCommand, null)
        } else {
            mainHandler.post { webView.evaluateJavascript(javascriptCommand, null) }
        }
    }

    /**
     * 分发message 必须在主线程才分发成功
     *
     */
    private fun dispatchMessage(handlerName: String, data: String, callbackId: String) {
        var messageJson = data
        //escape special characters for json string  为json字符串转义特殊字符
        messageJson = messageJson.replace("(\\\\)([^utrn])".toRegex(), "\\\\\\\\$1$2")
        messageJson = messageJson.replace("(?<=[^\\\\])(\")".toRegex(), "\\\\\"")
        messageJson = messageJson.replace("(?<=[^\\\\])(\')".toRegex(), "\\\\\'")
        messageJson = messageJson.replace("%7B".toRegex(), URLEncoder.encode("%7B"))
        messageJson = messageJson.replace("%7D".toRegex(), URLEncoder.encode("%7D"))
        messageJson = messageJson.replace("%22".toRegex(), URLEncoder.encode("%22"))
        val javascriptCommand =
            String.format(
                BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA,
                handlerName,
                messageJson,
                callbackId
            )
        Logger.d(javaClass.simpleName, "javascriptCommand->$javascriptCommand")

        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            webView.evaluateJavascript(javascriptCommand, null)
        } else {
            mainHandler.post { webView.evaluateJavascript(javascriptCommand, null) }
        }
    }


    /**
     * 保存message到消息队列
     *
     * @param handlerName      handlerName
     * @param data             data
     * @param responseCallback OnBridgeCallback
     */
    fun callHandler(
        handlerName: String,
        data: String,
        responseCallback: ((String) -> Unit)? = null
    ) {
        require(handlerName.isNotEmpty()) { "Empty handler name." }
        var callbackId = ""
        if (responseCallback != null) {
            callbackId = String.format(
                BridgeUtil.CALLBACK_ID_FORMAT,
                "${mUniqueId.incrementAndGet()}" + BridgeUtil.UNDERLINE_STR + "${SystemClock.currentThreadTimeMillis()}"
            )
            mCallbacks[callbackId] = responseCallback
        }
        val request = mRequests
        if (request != null) {
            //等待状态添加到消息集合否则分发消息
            val jsRequest = JSRequest(callbackId, handlerName, data)
            request.add(jsRequest)
        } else {
            dispatchMessage(handlerName, data, callbackId)
        }
    }

    /**
     * 这里只是加载lib包中assets中的 WebViewJavascriptBridge.js
     * @param view webview
     */
    private fun loadJs(view: IWebView) {
        val mill = measureTimeMillis {
            val context = view.getContext() ?: return
            val jsContent = assetFile2Str(context)
            view.loadUrl("javascript:$jsContent")
        }
        Logger.i(javaClass.simpleName, "Load js bridge file cost:$mill ms")
    }

    /**
     * 解析assets文件夹里面的代码,去除注释,取可执行的代码
     * @param c context
     * @return 可执行代码
     */
    private fun assetFile2Str(c: Context): String {
        try {
            c.assets.open(BridgeUtil.JAVA_SCRIPT)
                .bufferedReader()
                .use {
                    val sb = StringBuilder()
                    val regex = Regex("^\\s*//.*")
                    do {
                        val line = it.readLine()
                        if (line?.matches(regex) != true) sb.append(line)
                    } while (line != null)
                    return sb.toString()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}

data class JSRequest(
    val callbackId: String,
    val handlerName: String,
    val data: String
)