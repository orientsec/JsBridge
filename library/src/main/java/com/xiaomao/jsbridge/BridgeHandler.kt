package com.xiaomao.jsbridge

interface BridgeHandler {
    fun handle(data: String, callback: (String) -> Unit)
}