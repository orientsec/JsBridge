package com.orientsec.jsbridge

interface IJSBridge {
    /**
     * Register handler, so that javascript can call it.
     * 注册处理程序,以便javascript调用它。
     *
     * @param handlerName HandlerName.
     * @param handler     BridgeHandler.
     */
    fun registerHandler(handlerName: String, handler: BridgeHandler)

    /**
     * Register handler,so that javascript can call it.
     * 注册处理程序,以便javascript调用它。
     *
     * @param handlers HandlerName.
     */
    fun registerHandler(handlers: Map<String, BridgeHandler>)

    /**
     * Unregister handler.
     *
     * @param handlerName HandlerName.
     */
    fun unregisterHandler(handlerName: String)

    /**
     * 调用Js handler。
     *
     * @param handlerName      HandlerName.
     * @param data             Request data.
     * @param responseCallback OnBridgeCallback.
     */
    fun callHandler(handlerName: String, data: String, responseCallback: ((String) -> Unit)? = null)
}