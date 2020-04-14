package com.orientsec.jsbridge

//空回调函数。当js请求的回调id为null时，不需要回调js。
private val EmptyResponseCallBack: (String) -> Unit = {}

/**
 * 原生指令handler。
 */
interface BridgeHandler {
    /**
     * 处理js请求。
     * @param data 请求数据。
     * @param callback 请求回调函数。
     */
    fun handle(data: String, callback: (String) -> Unit = EmptyResponseCallBack)
}