package com.orientsec.jsbridge

private val EmptyResponseCallBack: (String) -> Unit = {}

interface BridgeHandler {
    fun handle(data: String, callback: (String) -> Unit = EmptyResponseCallBack)
}