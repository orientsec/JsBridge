package com.orientsec.jsbridge

import android.os.SystemClock
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class JsBridgeDelegate(webView: BridgeWebView) : JsBridge, MessageListener,
    Loggable by BridgeLogger {
    private val channel: MessageChannel

    init {
        channel = if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            SafeMessageChannel(webView)
        } else {
            UnsafeMessageChannel(webView)
        }
        channel.addMessageListener(this)
    }

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
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlerName handlerName
     * @param handler     BridgeHandler
     */
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
    override fun registerHandler(handlers: Map<String, BridgeHandler>) {
        mMessageHandlers.putAll(handlers)
    }


    /**
     * unregister handler
     *
     * @param handlerName
     */
    override fun unregisterHandler(handlerName: String) {
        mMessageHandlers.remove(handlerName)
    }


    /**
     * 调用Js handler。
     *
     * @param handlerName      HandlerName.
     * @param data             Request data.
     * @param responseCallback BridgeCallback.
     */
    override fun callHandler(
        handlerName: String,
        data: String,
        responseCallback: BridgeCallback?
    ) {
        var callbackId = ""
        if (responseCallback != null) {
            callbackId = String.format(
                CALLBACK_ID_FORMAT,
                mUniqueId.incrementAndGet(),
                SystemClock.currentThreadTimeMillis()
            )
            mCallbacks[callbackId] = responseCallback
        }
        val request = Request(name = handlerName, data = data, callbackId = callbackId).toString()
        channel.postMessage(request)
    }

    override fun onMessage(message: String) {
        try {
            val jsonObject = JSONObject(message)
            when (val msg = MessageBuilder.create(jsonObject)) {
                is Request -> {
                    onRequest(msg.name, msg.data, msg.callbackId)
                }
                is Response -> {
                    onResponse(msg.code, msg.info, msg.data, msg.info)
                }
            }
        } catch (e: Exception) {
            error("Receive unexpected message: $message", e)
        }
    }

    private fun onRequest(handlerName: String, data: String, callbackId: String?) {
        info("requestFromJs->$handlerName, $data, callbackId: $callbackId")
        val callback = if (callbackId.isNullOrEmpty()) EmptyBridgeCallback
        else DispatchCallback(callbackId, channel)
        val handler = mMessageHandlers[handlerName]
        if (handler == null) {
            warn("No handler for [$handlerName].")
            callback.onError(-1, "Handler for [$handlerName] not found.")
        } else {
            handler.handle(data, callback)
        }
    }

    private fun onResponse(code: Int, info: String, data: String, callbackId: String) {
        info("responseFromJs->code[$code], info[$info], data[$data], callbackId[$callbackId]")
        mCallbacks.remove(callbackId)?.apply {
            if (code == 0) onResult(data)
            else onError(code, info)
        }
    }

    companion object {
        private const val CALLBACK_ID_FORMAT = "JAVA_CB_%s_%s"
    }
}

class DispatchCallback(
    private val callbackId: String,
    private val channel: MessageChannel
) : BridgeCallback {
    override fun onResult(data: String) {
        channel.postMessage(response(0, "OK", data, callbackId))
    }

    override fun onError(code: Int, info: String) {
        channel.postMessage(response(code, info, "", callbackId))
    }

    private fun response(
        code: Int,
        info: String,
        data: String,
        callbackId: String
    ): String {
//        val formattedData = data.replace("\\", "\\\\")
        return Response(code = code, info = info, data = data, callbackId = callbackId).toString()
    }
}

interface Message

interface MessageBuilder {
    fun create(jsonObject: JSONObject): Message

    companion object : MessageBuilder {
        override fun create(jsonObject: JSONObject): Message {
            return when (val type = jsonObject.getString("type")) {
                "request" -> Request.create(jsonObject)
                "response" -> Response.create(jsonObject)
                else -> throw IllegalArgumentException("Unknown message type $type")
            }
        }
    }
}

class Request(val name: String, val data: String = "", val callbackId: String?) :
    Message {
    override fun toString(): String {
        val jsonObject = JSONObject(
            mapOf(
                "type" to "request",
                "name" to name,
                "data" to data,
                "callbackId" to callbackId
            )
        )
        return jsonObject.toString()
    }

    companion object : MessageBuilder {
        override fun create(jsonObject: JSONObject): Message {
            require(jsonObject.getString("type") == "request") { "Not a request message." }
            val name = jsonObject.getString("name")
            val data = jsonObject.getString("data")
            val callbackId = jsonObject.getString("callbackId")
            return Request(name, data, callbackId)
        }
    }
}

class Response(
    val code: Int = 0,
    val info: String = "OK",
    val data: String = "",
    val callbackId: String
) : Message {

    override fun toString(): String {
        val jsonObject = JSONObject(
            mapOf(
                "type" to "response",
                "code" to code,
                "info" to info,
                "data" to data,
                "callbackId" to callbackId
            )
        )
        return jsonObject.toString()
    }

    companion object : MessageBuilder {
        override fun create(jsonObject: JSONObject): Message {
            require(jsonObject.getString("type") == "response") { "Not a response message." }
            val code = jsonObject.getInt("code")
            val info = jsonObject.getString("info")
            val data = jsonObject.getString("data")
            val callbackId = jsonObject.getString("callbackId")
            return Response(code, info, data, callbackId)
        }
    }
}