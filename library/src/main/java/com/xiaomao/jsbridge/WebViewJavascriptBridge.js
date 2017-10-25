//use http://closure-compiler.appspot.com/home to generate jsbridge file
//notation: js file can only use this kind of comments
//since comments will cause error when use in webview.loadurl,
//comments will be remove by java use regexp
(function () {
    if (window.WebViewJavascriptBridge) {
        return;
    }

    var receiveRequestQueue = [];
    var requestHandlers = {};

    var CUSTOM_PROTOCOL_SCHEME = 'gl://';
    var GL_REQUEST = CUSTOM_PROTOCOL_SCHEME + 'request/';
    var GL_RESPONSE = CUSTOM_PROTOCOL_SCHEME + 'response/';

    var responseCallbacks = {};
    var uniqueId = 1;

    //set default requestHandler
    function init(requestHandler) {
        if (WebViewJavascriptBridge._requestHandler) {
            throw new Error('WebViewJavascriptBridge.init called twice');
        }
        WebViewJavascriptBridge._requestHandler = requestHandler;
        var receivedRequests = receiveRequestQueue;
        receiveRequestQueue = null;
        for (var i = 0; i < receivedRequests.length; i++) {
            _dispatchRequestFromNative(receivedRequests[i]);
        }
    }

    function send(data, responseCallback) {
        _doSend({
            data: data
        }, responseCallback);
    }

    function registerHandler(handlerName, handler) {
        requestHandlers[handlerName] = handler;
    }

    function callHandler(handlerName, data, responseCallback) {
        _doSend({
            handlerName: handlerName,
            data: data
        }, responseCallback);
    }

    //sendRequest
    function _doSend(request, responseCallback) {
        if (responseCallback) {
            var callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
            request.id = callbackId;
        }
        prompt(GL_REQUEST + JSON.stringify(request));
    }

    function _handleResponseFromNative(responseJson) {
        setTimeout(function () {
            var response = JSON.parse(responseJson);
            var responseCallback = responseCallbacks[response.id];
            if (!responseCallback) {
                return;
            }
            responseCallback(response.data);
            delete responseCallbacks[response.id];
        });
    }

    //提供给native使用,
    function _dispatchRequestFromNative(requestJSON) {
        setTimeout(function () {
            var request = JSON.parse(requestJSON);
            var responseCallback;
            if (request.id) {
                responseCallback = function (responseData) {
                    var response = {
                        id: request.id,
                        data: responseData
                    };
                    prompt(GL_RESPONSE + JSON.stringify(response));
                }
            } else {
                responseCallback = function (responseData) {

                }
            }
            //查找指定handler
            var handler;
            if (request.handlerName) {
                handler = requestHandlers[request.handlerName];
            } else {
                handler = WebViewJavascriptBridge._requestHandler;
            }
            if (handler == undefined) {
                console.log("WebViewJavascriptBridge: WARNING:no handler for ", request.handlerName);
            } else {
                try {
                    handler(request.data, responseCallback);
                } catch (exception) {
                    console.error('WebViewJavascriptBridge: WARNING: javascript handler threw.', exception.stack);
                }
            }
        });
    }

    //提供给native调用,receiveRequestQueue 在会在页面加载完后赋值为null,所以
    function _handleRequestFromNative(requestJSON) {
        //console.log(requestJSON);
        if (receiveRequestQueue) {
            receiveRequestQueue.push(requestJSON);
        } else {
            _dispatchRequestFromNative(requestJSON);
        }
    }

    var WebViewJavascriptBridge = window.WebViewJavascriptBridge = {
        init: init,
        send: send,
        registerHandler: registerHandler,
        callHandler: callHandler,
        _handleRequestFromNative: _handleRequestFromNative,
        _handleResponseFromNative: _handleResponseFromNative
    };

    var doc = document;
    var readyEvent = doc.createEvent('Events');
    readyEvent.initEvent('WebViewJavascriptBridgeReady');
    readyEvent.bridge = WebViewJavascriptBridge;
    doc.dispatchEvent(readyEvent);
    var state = window.location.host.length > 0 ? "ok" : "error";
    prompt(CUSTOM_PROTOCOL_SCHEME + 'pageLoaded/' + state);
})();
