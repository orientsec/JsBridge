package com.orientsec.jsbridge

import android.os.SystemClock
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.iterator

/**
 * Internal class responsible for handling JavaScript bridge operations.
 *
 * @param messageChannel The message channel used for communication between JavaScript
 * @param pageOnLoadListener The listener for page loading events.
 * and native code.
 */
internal class JsBridgeInternal(
    private val messageChannel: MessageChannel,
    private val pageOnLoadListener: PageOnLoadListener
) : JsBridge,

    MessageListener,
    Loggable by BridgeLogger {

    /**
     * Unique ID for Native callbacks. Auto-incrementing.
     */
    private val mUniqueId = AtomicLong(0)

    /**
     * List of cached Native requests before the page is loaded.
     */
    private var requestList: MutableList<Request>? = mutableListOf()

    /**
     * Handlers for JavaScript to call Native methods.
     */
    private val mMessageHandlers: MutableMap<String, BridgeHandler> =
        mutableMapOf<String, BridgeHandler>()

    /**
     * Flag to indicate whether the JavaScript queue is being executed for the first time.
     */
    private val firstJsQueue = AtomicBoolean(true)

    /**
     * Native callbacks to receive results from JavaScript.
     */
    private val mCallbacks: MutableMap<String, BridgeCallback> = ConcurrentHashMap()

    /**
     *  mutable set to store request IDs, ensuring uniqueness and cleanup when the page ends.
     */
    private val jsCallbackIds: MutableSet<String> = mutableSetOf<String>()

    /**
     * Installs this JsBridge to the WebView.
     */
    fun install() {
        messageChannel.addMessageListener(this)
        messageChannel.active()
        registerHandler("_JS_BRIDGE_INIT") { _, callback ->
            callback.onSuccess(InitResponse(JsBridge.debug).toString())
            pageOnLoadListener.onFinish()
            dispatchStartupJsCall()
        }
    }

    /**
     * Registers a handler so that JavaScript can call it.
     *
     * @param name The name of the handler.
     * @param handler The BridgeHandler instance.
     */
    override fun registerHandler(name: String, handler: BridgeHandler) {
        // Add to the Map<String, BridgeHandler>
        synchronized(this) {
            mMessageHandlers[name] = handler
        }
    }

    /**
     * Registers multiple handlers so that JavaScript can call them.
     *
     * @param handlers A map of handler names to BridgeHandler instances.
     */
    override fun registerHandler(handlers: Map<String, BridgeHandler>) {
        synchronized(this) {
            mMessageHandlers.putAll(handlers)
        }
    }

    /**
     * Unregisters a handler.
     *
     * @param name The name of the handler to unregister.
     */
    override fun unregisterHandler(name: String) {
        synchronized(this) {
            mMessageHandlers.remove(name)
        }
    }

    /**
     * Calls a JavaScript handler.
     *
     * @param name The name of the handler.
     * @param data The request data.
     * @param responseCallback The BridgeCallback to receive the response.
     */
    override fun callHandler(
        name: String,
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
        val request = Request(name = name, data = data, callbackId = callbackId)
        requestList?.apply {
            add(request)
            debug("queueJsCall, queue size = $size")
        } ?: apply {
            debug("dispatchJsCall, request: ${request.name}")
            messageChannel.postMessage(request.serialize())
        }
    }

    /**
     * Resets the request queue.
     *
     * This function aims to reset the request queue for JS interactions, ensuring it can be
     * cleared and restarted under specific conditions.
     * For the first queue processing, it sets a flag to determine if a reset operation is needed.
     */
    fun resetRequestQueue() {
        // Attempt to change the firstJsQueue flag from true to false. If successful, it indicates the first queue processing.
        if (!firstJsQueue.compareAndSet(true, false)) {
            // Remove all Native callbacks
            mCallbacks.clear()
            // Reset requestList to prepare for new JS call requests
            requestList = mutableListOf()
            // Clear the JS callback ID list to ensure no residual callback information remains
            jsCallbackIds.clear()
        }
    }

    /**
     * Destroys all message handlers and releases related resources.
     *
     * This function ensures that all internal message handler resources are properly cleaned up
     * at the end of the object's lifecycle.
     * If a message handler implements the [Destroyable] interface, its destroy method is called
     * for cleanup.
     */
    fun destroy() {
        synchronized(this) {
            for (it in mMessageHandlers) {
                if (it.value is Destroyable) {
                    (it.value as Destroyable).destroy()
                }
            }
        }
    }

    /**
     * Calls JavaScript with requests that were made before the JsBridge was ready.
     */
    private fun dispatchStartupJsCall() {
        val requests = requestList
        debug("dispatchStartupJsCall, request list size = ${requests?.size}")
        requestList = null
        requests?.forEach { request ->
            messageChannel.postMessage(request.serialize())
        }
    }

    override fun onMessage(message: String) {
        try {
            when (val msg = Message.create(message)) {
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

    /**
     * Handles a request from JavaScript.
     *
     * @param name The name of the handler to be called.
     * @param data The data sent from JavaScript.
     * @param callbackId The callback ID used to send the response back to JavaScript,
     * if applicable.
     */
    private fun onRequest(name: String, data: String, callbackId: String?) {
        info("requestFromJs->$name, $data, callbackId: $callbackId")
        val callback = if (callbackId.isNullOrEmpty()) EmptyBridgeCallback
        else if (jsCallbackIds.add(callbackId)) {
            DispatchCallback(callbackId, messageChannel, jsCallbackIds)
        } else {
            warn("CallbackId $callbackId already exists.")
            DispatchCallback(callbackId, messageChannel, null)
                .onError(-2, "CallbackId $callbackId already exists.")
            return
        }

        val handler = synchronized(this) {
            mMessageHandlers[name]
        }
        if (handler == null) {
            warn("No handler for [$name].")
            callback.onError(-1, "Handler for [$name] not found.")
        } else {
            handler.handle(data, callback)
        }
    }

    /**
     * Handles a response to a previous request.
     *
     * @param code The response code indicating the result of the operation.
     * @param info Additional information or message related to the response.
     * @param data The data returned from the operation.
     * @param callbackId The callback ID used to identify the corresponding request.
     */
    private fun onResponse(code: Int, info: String, data: String, callbackId: String) {
        info("responseFromJs->code[$code], info[$info], data[$data], callbackId[$callbackId]")
        mCallbacks.remove(callbackId)?.apply {
            if (code == 0) onSuccess(data)
            else onError(code, info)
        }
    }

    companion object {
        private const val CALLBACK_ID_FORMAT = "JAVA_CB_%s_%s"
    }
}

// Empty callback function. When the callback ID from a JavaScript request is null,
// no callback to JavaScript is needed.
object EmptyBridgeCallback : BridgeCallback {
    override fun onSuccess(data: String) {
        BridgeLogger.info("Empty js callback, onResult:$data")
    }

    override fun onError(code: Int, info: String) {
        BridgeLogger.info("Empty js callback, onError:[$code, $info]")
    }
}

/**
 * DispatchCallback implements BridgeCallback, which sends the result of a Native handler to
 * JavaScript.
 * @param callbackId The callback ID.
 * @param jsCallbackIds The set of JavaScript callback IDs. If not null, the callback will only
 * be invoked if the callback ID is in the set, and the callback ID will be removed.
 */
private class DispatchCallback(
    val callbackId: String,
    val messageChannel: MessageChannel,
    val jsCallbackIds: MutableSet<String>?
) : BridgeCallback {
    override fun onSuccess(data: String) {
        postMessage(0, "OK", data)
    }

    override fun onError(code: Int, info: String) {
        postMessage(code, info, "")
    }

    private fun postMessage(code: Int, info: String, data: String) {
        if (jsCallbackIds == null || jsCallbackIds.remove(callbackId)) {
            val msg = Response(
                code = code,
                info = info,
                data = data,
                callbackId = callbackId
            ).serialize()
            messageChannel.postMessage(msg)
        }
    }
}

private data class InitResponse(val debug: Boolean) {
    override fun toString(): String {
        return JSONObject().put("debug", debug).toString()
    }
}
