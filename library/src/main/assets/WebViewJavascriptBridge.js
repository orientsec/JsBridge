(function () {
    if (window.WebViewJavascriptBridge) {
        return;
    }
    callHandler("onPageLoad", window.location.host);

    var receiveMessageQueue = [];
    var messageHandlers = {};

    var responseCallbacks = {};
    var uniqueId = 1;

    //set default messageHandler  初始化默认的消息线程
    function init(messageHandler) {
        if (WebViewJavascriptBridge._messageHandler) {
            throw new Error('WebViewJavascriptBridge.init called twice');
        }
        WebViewJavascriptBridge._messageHandler = messageHandler;
        var receivedMessages = receiveMessageQueue;
        receiveMessageQueue = null;
        for (var i = 0; i < receivedMessages.length; i++) {
            var message = receivedMessages[i];
            _dispatchMessageFromNative(message.handlerName, message.data, message.callbackId);
        }
    }

    // 注册js handler
    function registerHandler(handlerName, handler) {
        messageHandlers[handlerName] = handler;
    }

    // 调用原生handler
    function callHandler(handlerName, data, responseCallback) {
        var callbackId;
        if (responseCallback) {
            callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
        } else {
            callbackId = null;
        }

        try {
            //调用原生JavascriptInterface
            window.jsBridge.callHandler(handlerName, JSON.stringify(data), callbackId);
        } catch (e) {
            console.log(e);
        }
    }

    //向原生发送响应结果
    function _responseToNative(data, callbackId) {
        try {
            window.jsBridge.response(JSON.stringify(data), callbackId);
        } catch (e) {
            console.log(e);
        }
    }

    //提供给native使用
    function _responseFromNative(data, callbackId) {
        setTimeout(function () {
            var responseCallback = responseCallbacks[callbackId];
            if (!responseCallback) {
                return;
            }
            responseCallback(data);
            delete responseCallbacks[callbackId];
        });
    }

    function _dispatchMessageFromNative(handlerName, data, callbackId) {
        setTimeout(function () {
            var responseCallback;
            //直接发送
            if (callbackId) {
                responseCallback = function (responseData) {
                    _responseToNative(responseData, callbackId);
                };
            }
            //查找指定handler
            var handler;
            if (handlerName) {
                handler = messageHandlers[handlerName];
            } else {
                handler = WebViewJavascriptBridge._requestHandler;
            }
            if (handler == undefined) {
                console.log("WebViewJavascriptBridge: WARNING:no handler for ", request.handlerName);
            } else {
                try {
                    handler(data, responseCallback);
                } catch (exception) {
                    console.error('WebViewJavascriptBridge: WARNING: javascript handler threw.', exception.stack);
                }
            }
        });
    }

    //提供给native调用,receiveMessageQueue 在会在页面加载完后赋值为null,所以
    function _handleMessageFromNative(handlerName, data, callbackId) {
        console.log('handle message: ' + handlerName + ' callbackId:' + callbackId +" data:" + data);
        if (receiveMessageQueue) {
            var messageJson = {handlerName: handlerName, data: data, callbackId: callbackId};
            receiveMessageQueue.push(messageJson);
        }
        _dispatchMessageFromNative(handlerName, data, callbackId);

    }

    var WebViewJavascriptBridge = window.WebViewJavascriptBridge = {
        init: init,
        registerHandler: registerHandler,
        callHandler: callHandler,
        _handleMessageFromNative: _handleMessageFromNative,
        _responseFromNative: _responseFromNative
    };

    var doc = document;
    var readyEvent = doc.createEvent('Events');
    readyEvent.initEvent('WebViewJavascriptBridgeReady');
    readyEvent.bridge = WebViewJavascriptBridge;
    doc.dispatchEvent(readyEvent);
})();