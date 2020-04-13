package com.orientsec.jsbridge

object BridgeUtil {
    const val JAVA_SCRIPT = "WebViewJavascriptBridge.js"
    const val CALLBACK_ID_FORMAT = "JAVA_CB_%s"
    const val JS_HANDLE_MESSAGE_FROM_JAVA =
        "javascript:WebViewJavascriptBridge._handleMessageFromNative('%s','%s','%s');"
    const val JS_RESPONSE_FROM_JAVA =
        "javascript:WebViewJavascriptBridge._responseFromNative('%s','%s');"
    const val UNDERLINE_STR = "_"
}
