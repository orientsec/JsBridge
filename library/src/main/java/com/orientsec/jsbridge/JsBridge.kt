package com.orientsec.jsbridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.webkit.JavascriptInterface
import androidx.annotation.MainThread
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

class JsBridge(private val webView: IWebView) : IJSBridge,
    Loggable by Logger(JsBridge::class.java.simpleName) {

    enum class State {
        IDLE, READY, DESTROY
    }

    private var state: State = State.IDLE

    private val mUniqueId = AtomicLong(0)

    private val mMessageHandlers: MutableMap<String, BridgeHandler> = ConcurrentHashMap()

    private val mCallbacks: MutableMap<String, (String) -> Unit> = ConcurrentHashMap()

    private val mRequests: MutableList<String> = ArrayList()

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
    @MainThread
    override fun registerHandler(handlerName: String, handler: BridgeHandler) {
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
    @MainThread
    override fun registerHandler(handlers: Map<String, BridgeHandler>) {
        checkState()
        mMessageHandlers.putAll(handlers)
    }


    /**
     * unregister handler
     *
     * @param handlerName
     */
    @MainThread
    override fun unregisterHandler(handlerName: String) {
        checkState()
        mMessageHandlers.remove(handlerName)
    }


    @JavascriptInterface
    fun callHandler(handlerName: String, data: String, callbackId: String?) {
        info(
            "callJavaHandler->$handlerName, $data, callbackId: " +
                    "$callbackId [${Thread.currentThread().name}]"
        )
        val handler = mMessageHandlers[handlerName]
        if (handler == null) {
            warn("no handler for:$handlerName")
            return
        }
        if (callbackId == null) {
            handler.handle(data)
        } else {
            handler.handle(data) {
                dispatchMessage(String.format(JS_RESPONSE_FROM_JAVA, it, callbackId))
            }
        }
    }

    @JavascriptInterface
    fun response(data: String, callbackId: String) {
        info("responseFromJs->$data, callbackId: $callbackId [${Thread.currentThread().name}]")
        mCallbacks.remove(callbackId)?.invoke(data)
    }

    @MainThread
    fun register(view: IWebView) {
        if (state != State.IDLE) return
        state = State.READY
        registerHandler("onPageLoad", object : BridgeHandler {
            override fun handle(data: String, callback: (String) -> Unit) {
                runOnUiThread { view.onPageLoadListener.onPageLoaded(data.isNotEmpty()) }
            }
        })
        loadJs(view)
        if (mRequests.isNotEmpty()) {
            dispatchMessage(mRequests)
            mRequests.clear()
        }
    }

    /**
     * Free memory.
     */
    @MainThread
    fun clean() {
        if (state < State.DESTROY) {
            state = State.DESTROY
            mCallbacks.clear()
            mMessageHandlers.clear()
            mRequests.clear()
        }
    }

    /**
     * 调用Js handler。
     *
     * @param handlerName      HandlerName.
     * @param data             Request data.
     * @param responseCallback OnBridgeCallback.
     */
    @MainThread
    override fun callHandler(
        handlerName: String,
        data: String,
        responseCallback: ((String) -> Unit)?
    ) {
        checkState()
        require(handlerName.isNotEmpty()) { "Empty handler name." }
        var callbackId = ""
        if (responseCallback != null) {
            callbackId = String.format(
                CALLBACK_ID_FORMAT,
                mUniqueId.incrementAndGet(),
                SystemClock.currentThreadTimeMillis()
            )
            mCallbacks[callbackId] = responseCallback
        }
        val jsCommand = String.format(JS_HANDLE_MESSAGE_FROM_JAVA, handlerName, data, callbackId)
        if (state == State.IDLE) {
            //等待状态添加到消息集合否则分发消息
            mRequests.add(jsCommand)
        } else {
            dispatchMessage(jsCommand)
        }
    }

    /**
     * 这里只是加载lib包中assets中的 WebViewJavascriptBridge.js。
     * @param view WebView.
     */
    private fun loadJs(view: IWebView) {
        val mill = measureTimeMillis {
            val context = view.getContext() ?: return
            val jsContent = assetFile2Str(context)
            view.loadUrl("javascript:$jsContent")
        }
        info("load js bridge file cost:$mill ms")
    }

    /**
     * 解析assets文件夹里面的代码,去除注释,取可执行的代码。
     * @param c Context.
     * @return 可执行代码。
     */
    private fun assetFile2Str(c: Context): String {
        try {
            c.assets.open(JAVA_SCRIPT)
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

    private fun dispatchMessage(jsCommand: String) {
        runOnUiThread {
            info("callJs->$jsCommand")
            webView.evaluateJavascript(jsCommand, null)
        }
    }

    private fun dispatchMessage(jsCommands: List<String>) {
        runOnUiThread {
            jsCommands.forEach {
                info("callJs->$it")
                webView.evaluateJavascript(it, null)
            }
        }
    }

    private fun runOnUiThread(runnable: () -> Unit) {
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            if (state == State.READY) {
                runnable()
            } else {
                warn("JsBridge is destroyed, stop dispatch to js")
            }
        } else {
            mainHandler.post {
                if (state == State.READY) {
                    runnable()
                } else {
                    warn("JsBridge is destroyed, stop dispatch to js")
                }
            }
        }
    }

    companion object {
        const val JAVA_SCRIPT = "WebViewJavascriptBridge.js"
        const val CALLBACK_ID_FORMAT = "JAVA_CB_%s_%s"
        const val JS_HANDLE_MESSAGE_FROM_JAVA =
            "javascript:WebViewJavascriptBridge._handleMessageFromNative('%s','%s','%s');"
        const val JS_RESPONSE_FROM_JAVA =
            "javascript:WebViewJavascriptBridge._responseFromNative('%s','%s');"
        var debug: Boolean = false
    }
}