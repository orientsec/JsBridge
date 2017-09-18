package com.xiaomao.jsbridge;


public interface WebViewJavascriptBridge {
    void send(String data);

    void send(String data, CallBackFunction responseCallback);

    void registerHandler(String handlerName, BridgeHandler handler);

    void callHandler(String handlerName, String data, CallBackFunction callBack);
}
