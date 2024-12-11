package com.xiaomao.jsbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class BridgeWebView extends WebView implements WebViewJavascriptBridge, OnPageLoadListener {
    private OnPageLoadListener onPageLoadListener;
    private final Map<String, CallBackFunction> responseCallbacks = new HashMap<>();
    private final Map<String, BridgeHandler> messageHandlers = new HashMap<>();
    private BridgeHandler defaultHandler = new DefaultHandler();

    private List<Request> startupRequests = new ArrayList<>();

    private BridgeWebViewClient bridgeWebViewClient;

    private long uniqueId = 0;

    public void setOnPageLoadListener(OnPageLoadListener onPageLoadListener) {
        this.onPageLoadListener = onPageLoadListener;
    }

    public BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public BridgeWebView(Context context) {
        super(context);
        init();
    }

    /**
     * @param handler default handler,handle messages send by js without assigned handler name,
     *                if js message has handler name, it will be handled by named handlers registered by native
     */
    public void setDefaultHandler(BridgeHandler handler) {
        this.defaultHandler = handler;
    }

    private void init() {
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        this.getSettings().setJavaScriptEnabled(true);
        WebView.setWebContentsDebuggingEnabled(true);
        bridgeWebViewClient = new BridgeWebViewClient();
        super.setWebViewClient(bridgeWebViewClient);
        addJavascriptInterface(this, "jsBridge");
    }

    public void onPageLoaded(boolean isLoaded) {
        if (onPageLoadListener != null) {
            onPageLoadListener.onPageLoaded(isLoaded);
        }
    }

    @Override
    public void setWebViewClient(@NonNull WebViewClient client) {
        bridgeWebViewClient.setWebViewClient(client);
    }

    void sendStartupRequests() {
        if (startupRequests != null) {
            for (Request request : startupRequests) {
                dispatchRequest(request);
            }
            startupRequests = null;
        }
    }

    @Override
    public void send(String data) {
        send(data, null);
    }

    @Override
    public void send(String data, CallBackFunction responseCallback) {
        doSend(null, data, responseCallback);
    }

    private void doSend(String handlerName, String data, CallBackFunction responseCallback) {
        Request request = new Request();
        if (!TextUtils.isEmpty(data)) {
            request.setData(data);
        }
        if (responseCallback != null) {
            String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            responseCallbacks.put(callbackStr, responseCallback);
            request.setId(callbackStr);
        }
        if (!TextUtils.isEmpty(handlerName)) {
            request.setHandlerName(handlerName);
        }
        if (startupRequests != null) {
            startupRequests.add(request);
        } else {
            dispatchRequest(request);
        }
    }

    void dispatchRequest(Request request) {
        String requestJson = request.toJson();
        //escape special characters for json string
        requestJson = requestJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        requestJson = requestJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_REQUEST_FROM_JAVA, requestJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }

    void dispatchResponse(Response response) {
        String responseJson = response.toJson();
        //escape special characters for json string
        responseJson = responseJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        responseJson = responseJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_RESPONSE_FROM_JAVA, responseJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }

    void handleResponse(Response response) {
        CallBackFunction function = responseCallbacks.get(response.getId());
        if (function != null) {
            function.onCallBack(response.getData());
            responseCallbacks.remove(response.getId());
        }
    }

    void handleRequest(final Request request) {
        CallBackFunction responseFunction;
        // if had callbackId
        if (!TextUtils.isEmpty(request.getId())) {
            responseFunction = data -> {
                Response response = new Response();
                response.setId(request.getId());
                response.setData(data);
                dispatchResponse(response);
            };
        } else {
            responseFunction = data -> {
                // do nothing
            };
        }
        BridgeHandler handler;
        if (!TextUtils.isEmpty(request.getHandlerName())) {
            handler = messageHandlers.get(request.getHandlerName());
        } else {
            handler = defaultHandler;
        }
        if (handler != null) {
            handler.handler(request.getData(), responseFunction);
        }
    }

    /**
     * register handler,so that javascript can call it
     *
     * @param handlerName handler name
     * @param handler js handler
     */
    public void registerHandler(String handlerName, BridgeHandler handler) {
        if (handler != null) {
            messageHandlers.put(handlerName, handler);
        }
    }

    /**
     * call javascript registered handler
     *
     * @param handlerName handler name
     * @param data data
     * @param callBack callback
     */
    public void callHandler(String handlerName, String data, CallBackFunction callBack) {
        doSend(handlerName, data, callBack);
    }


    @JavascriptInterface
    public void onMessage(String message) {
        if (message.startsWith(BridgeUtil.GL_REQUEST)) {
            final Request request = Request.toObject(message.replace(BridgeUtil.GL_REQUEST, ""));
            if (request != null) {
                post(() -> handleRequest(request));
            }
        } else if (message.startsWith(BridgeUtil.GL_RESPONSE)) {
            final Response response = Response.toObject(message.replace(BridgeUtil.GL_RESPONSE, ""));
            if (response != null) {
                post(() -> handleResponse(response));
            }
        } else if (message.startsWith(BridgeUtil.GL_PAGE_LOADED)) {
            String result = message.replace(BridgeUtil.GL_PAGE_LOADED, "");
            post(() -> onPageLoaded("ok".equals(result)));
        }
    }
}
