package com.xiaomao.jsbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class BridgeWebView extends WebView implements WebViewJavascriptBridge, OnPageLoadListener {
    private OnPageLoadListener onPageLoadListener;
    private Map<String, CallBackFunction> responseCallbacks = new HashMap<String, CallBackFunction>();
    private Map<String, BridgeHandler> messageHandlers = new HashMap<>();
    private BridgeHandler defaultHandler = new DefaultHandler();

    private List<Request> startupRequests = new ArrayList<>();

    private BridgeWebViewClient bridgeWebViewClient;
    private BridgeWebChromeClient bridgeWebChromeClient;

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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BridgeWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Deprecated
    public BridgeWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        bridgeWebViewClient = new BridgeWebViewClient();
        bridgeWebChromeClient = new BridgeWebChromeClient();
        super.setWebViewClient(bridgeWebViewClient);
        super.setWebChromeClient(bridgeWebChromeClient);
    }

    public void onPageLoaded(boolean isLoaded) {
        if (onPageLoadListener != null) {
            onPageLoadListener.onPageLoaded(isLoaded);
        }
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        bridgeWebViewClient.setWebViewClient(client);
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        bridgeWebChromeClient.setWebChromeClient(client);
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
            responseFunction = new CallBackFunction() {
                @Override
                public void onCallBack(String data) {
                    Response response = new Response();
                    response.setId(request.getId());
                    response.setData(data);
                    dispatchResponse(response);
                }
            };
        } else {
            responseFunction = new CallBackFunction() {
                @Override
                public void onCallBack(String data) {
                    // do nothing
                }
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
     * @param handlerName
     * @param handler
     */
    public void registerHandler(String handlerName, BridgeHandler handler) {
        if (handler != null) {
            messageHandlers.put(handlerName, handler);
        }
    }

    /**
     * call javascript registered handler
     *
     * @param handlerName
     * @param data
     * @param callBack
     */
    public void callHandler(String handlerName, String data, CallBackFunction callBack) {
        doSend(handlerName, data, callBack);
    }


}
