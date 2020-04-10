package com.xiaomao.jsbridge

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.ArrayMap
import android.webkit.JavascriptInterface
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.HashMap

private val EmptyResponseCallBack: (String) -> Unit = {}


class JsBridge(private val webView: IWebView) {
    enum class State {
        IDLE, RUN, DESTROY
    }

    private var state: State = State.IDLE

    private val mUniqueId = AtomicLong(0)

    private val mMessageHandlers: MutableMap<String, BridgeHandler> = HashMap()

    private val mCallbacks: MutableMap<String, (String) -> Unit> = ArrayMap()

    private var mRequests: MutableList<JSRequest> = ArrayList()

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun checkState() {
        require(state < State.DESTROY) { "JsBridge is destroyed." }
    }

    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlerName handlerName
     * @param handler     BridgeHandler
     */
    @Synchronized
    fun registerHandler(handlerName: String, handler: BridgeHandler) {
        checkState()
        // 添加至 Map<String, BridgeHandler>
        mMessageHandlers[handlerName] = handler
    }


    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlers handlerName
     */
    @Synchronized
    fun registerHandler(handlers: Map<String, BridgeHandler>) {
        checkState()
        mMessageHandlers.putAll(handlers)
    }


    /**
     * unregister handler
     *
     * @param handlerName
     */
    @Synchronized
    fun unregisterHandler(handlerName: String) {
        checkState()
        mMessageHandlers.remove(handlerName)
    }


    @JavascriptInterface
    @Synchronized
    fun request(seqNo: String, handlerName: String, data: String) {
        mMessageHandlers[handlerName]?.handle(data) {
            dispatchMessage(JSResponse(seqNo, data))
        }
    }

    @JavascriptInterface
    @Synchronized
    fun response(data: String, seqNo: String) {
        Logger.d(
            javaClass.simpleName,
            "response->$data, seqNo: $seqNo ${Thread.currentThread().name}"
        )
        mCallbacks.remove(seqNo)?.invoke(data)
    }

    @Synchronized
    fun register(view: IWebView) {
        require(state == State.IDLE) { "JsBridge is registered." }
        state = State.RUN
        view.addJavascriptInterface(this, "jsbridge")
        view.loadUrl(
            String.format(
                BridgeUtil.JAVASCRIPT_STR,
                BridgeUtil.WebviewJavascriptBridge
            )
        )
        for (request in mRequests) {
            dispatchMessage(request)
        }
    }

    /**
     * free memory
     */
    @Synchronized
    fun clean() {
        state = State.DESTROY
        mCallbacks.clear()
        mMessageHandlers.clear()
        mRequests.clear()
    }

    /**
     * 分发message 必须在主线程才分发成功
     *
     * @param message Message
     */
    private fun dispatchMessage(message: Message) {
        var messageJson = message.toJson()
        //escape special characters for json string  为json字符串转义特殊字符
        messageJson = messageJson.replace("(\\\\)([^utrn])".toRegex(), "\\\\\\\\$1$2")
        messageJson = messageJson.replace("(?<=[^\\\\])(\")".toRegex(), "\\\\\"")
        messageJson = messageJson.replace("(?<=[^\\\\])(\')".toRegex(), "\\\\\'")
        messageJson = messageJson.replace("%7B".toRegex(), URLEncoder.encode("%7B"))
        messageJson = messageJson.replace("%7D".toRegex(), URLEncoder.encode("%7D"))
        messageJson = messageJson.replace("%22".toRegex(), URLEncoder.encode("%22"))
        val javascriptCommand =
            String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson)
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
    @Synchronized
    fun callHandler(
        handlerName: String,
        data: String,
        responseCallback: (String) -> Unit = EmptyResponseCallBack
    ) {
        checkState()
        require(handlerName.isNotEmpty()) { "Empty handler name." }
        val sequenceId: String = String.format(
            BridgeUtil.CALLBACK_ID_FORMAT,
            "${mUniqueId.incrementAndGet()}" + BridgeUtil.UNDERLINE_STR + "${SystemClock.currentThreadTimeMillis()}"
        )
        mCallbacks[sequenceId] = responseCallback
        val request = JSRequest(sequenceId, handlerName, data)
        //等待状态添加到消息集合否则分发消息
        if (state == State.IDLE) {
            mRequests.add(request)
        } else {
            dispatchMessage(request)
        }
    }

}