package com.orientsec.jsbridge

/**
 * 原生指令handler。
 */
fun interface BridgeHandler {
    /**
     * 处理js请求。
     * @param data 请求数据。
     * @param callback 请求回调函数。
     */
    fun handle(data: String, callback: BridgeCallback)
}

interface BridgeCallback {
    /**
     * 成功回调。
     */
    fun onResult(data: String)

    /**
     * 失败回调。
     */
    fun onError(code: Int, info: String)
}

//空回调函数。当js请求的回调id为null时，不需要回调js。
object EmptyBridgeCallback : BridgeCallback {
    override fun onResult(data: String) {
        BridgeLogger.info("None js callback, onResult:$data")
    }

    override fun onError(code: Int, info: String) {
        BridgeLogger.info("None js callback, onError:[$code, $info]")
    }
}