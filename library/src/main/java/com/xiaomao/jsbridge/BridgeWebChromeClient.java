package com.xiaomao.jsbridge;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;

class BridgeWebChromeClient extends WebChromeClient {
    private WebChromeClient webChromeClient = new WebChromeClient();

    public void setWebChromeClient(WebChromeClient webChromeClient) {
        this.webChromeClient = webChromeClient;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        webChromeClient.onProgressChanged(view, newProgress);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        webChromeClient.onReceivedTitle(view, title);
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        webChromeClient.onReceivedIcon(view, icon);
    }

    @Override
    public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
        webChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        webChromeClient.onShowCustomView(view, callback);
    }

    @Override
    public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
        webChromeClient.onShowCustomView(view, requestedOrientation, callback);
    }

    @Override
    public void onHideCustomView() {
        webChromeClient.onHideCustomView();
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        return webChromeClient.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
    }

    @Override
    public void onRequestFocus(WebView view) {
        webChromeClient.onRequestFocus(view);
    }

    @Override
    public void onCloseWindow(WebView window) {
        webChromeClient.onCloseWindow(window);
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        return webChromeClient.onJsAlert(view, url, message, result);
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        return webChromeClient.onJsConfirm(view, url, message, result);
    }

    @Override
    public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
        return webChromeClient.onJsBeforeUnload(view, url, message, result);
    }

    @Override
    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota, long estimatedDatabaseSize, long totalQuota, WebStorage.QuotaUpdater quotaUpdater) {
        webChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
    }

    @Override
    public void onReachedMaxAppCacheSize(long requiredStorage, long quota, WebStorage.QuotaUpdater quotaUpdater) {
        webChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        webChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
    }

    @Override
    public void onGeolocationPermissionsHidePrompt() {
        webChromeClient.onGeolocationPermissionsHidePrompt();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPermissionRequest(PermissionRequest request) {
        webChromeClient.onPermissionRequest(request);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPermissionRequestCanceled(PermissionRequest request) {
        webChromeClient.onPermissionRequestCanceled(request);
    }

    @Override
    public boolean onJsTimeout() {
        return webChromeClient.onJsTimeout();
    }

    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        webChromeClient.onConsoleMessage(message, lineNumber, sourceID);
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        return webChromeClient.onConsoleMessage(consoleMessage);
    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        return webChromeClient.getDefaultVideoPoster();
    }

    @Override
    public View getVideoLoadingProgressView() {
        return webChromeClient.getVideoLoadingProgressView();
    }

    @Override
    public void getVisitedHistory(ValueCallback<String[]> callback) {
        webChromeClient.getVisitedHistory(callback);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        return webChromeClient.onShowFileChooser(webView, filePathCallback, fileChooserParams);
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        if (interceptMessage((BridgeWebView) view, message)) {
            result.confirm();
            return true;
        } else {
            return webChromeClient.onJsPrompt(view, url, message, defaultValue, result);
        }
    }

    private boolean interceptMessage(final BridgeWebView view, String message) {
        if (message.startsWith(BridgeUtil.GL_REQUEST)) {
            final Request request = Request.toObject(message.replace(BridgeUtil.GL_REQUEST, ""));
            if (request != null) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        view.handleRequest(request);
                    }
                });
            }
            return true;
        } else if (message.startsWith(BridgeUtil.GL_RESPONSE)) {
            final Response response = Response.toObject(message.replace(BridgeUtil.GL_RESPONSE, ""));
            if (response != null) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        view.handleResponse(response);
                    }
                });
            }
            return true;
        } else if (message.startsWith(BridgeUtil.GL_PAGE_LOADED)) {
            String result = message.replace(BridgeUtil.GL_PAGE_LOADED, "");
            view.onPageLoaded("ok".equals(result));
            return true;
        } else {
            return false;
        }
    }
}
