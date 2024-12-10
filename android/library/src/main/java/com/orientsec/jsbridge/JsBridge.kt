package com.orientsec.jsbridge

interface JsBridge {
    /**
     * Register handler, so that javascript can call it.
     * 注册处理程序,以便javascript调用。
     *
     * @param name HandlerName.
     * @param handler BridgeHandler.
     */
    fun registerHandler(name: String, handler: BridgeHandler)

    /**
     * Register handler,so that javascript can call it.
     * 注册处理程序,以便javascript调用。
     *
     * @param handlers HandlerName.
     */
    fun registerHandler(handlers: Map<String, BridgeHandler>)

    /**
     * Unregister handler.
     *
     * @param name HandlerName.
     */
    fun unregisterHandler(name: String)

    /**
     * Call Javascript handler。
     *
     * @param name      HandlerName.
     * @param data             Request data.
     * @param responseCallback BridgeCallback.
     */
    fun callHandler(name: String, data: String, responseCallback: BridgeCallback? = null)

    companion object {
        var debug: Boolean = false
    }
}
