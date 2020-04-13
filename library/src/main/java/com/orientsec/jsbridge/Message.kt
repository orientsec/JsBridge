package com.orientsec.jsbridge

data class JSRequest(
    val callbackId: String?,
    val handlerName: String,
    val data: String
)