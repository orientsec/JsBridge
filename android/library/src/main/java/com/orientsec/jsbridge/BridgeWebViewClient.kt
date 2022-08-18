package com.orientsec.jsbridge

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.KeyEvent
import android.webkit.*
import androidx.annotation.RequiresApi

/**
 * 如果要自定义WebViewClient必须要集成此类。
 */
internal class BridgeWebViewClient : WebViewClient() {
    internal var webViewClient: WebViewClient = WebViewClient()

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("shouldOverrideUrlLoading(WebView, WebResourceRequest)")
    )
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        url: String?
    ): Boolean {
        return webViewClient.shouldOverrideUrlLoading(view, url)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        return webViewClient.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?
    ) {
        webViewClient.onPageStarted(view, url, favicon)
    }

    override fun shouldOverrideKeyEvent(
        view: WebView?,
        event: KeyEvent?
    ): Boolean {
        return webViewClient.shouldOverrideKeyEvent(view, event)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        webViewClient.onPageFinished(view, url)
        (view as BridgeWebView).loadJs()
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        webViewClient.onLoadResource(view, url)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onPageCommitVisible(view: WebView?, url: String?) {
        webViewClient.onPageCommitVisible(view, url)
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("shouldInterceptRequest(WebView, WebResourceRequest)")
    )
    override fun shouldInterceptRequest(
        view: WebView?,
        url: String?
    ): WebResourceResponse? {
        return webViewClient.shouldInterceptRequest(view, url)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        return webViewClient.shouldInterceptRequest(view, request)
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("")
    )
    override fun onTooManyRedirects(
        view: WebView?,
        cancelMsg: Message?,
        continueMsg: Message?
    ) {
        webViewClient.onTooManyRedirects(view, cancelMsg, continueMsg)
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("onReceivedError(WebView, WebResourceRequest, WebResourceError)")
    )
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        webViewClient.onReceivedError(view, errorCode, description, failingUrl)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        webViewClient.onReceivedError(view, request, error)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        webViewClient.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onFormResubmission(
        view: WebView?,
        dontResend: Message?,
        resend: Message?
    ) {
        webViewClient.onFormResubmission(view, dontResend, resend)
    }

    override fun doUpdateVisitedHistory(
        view: WebView?,
        url: String?,
        isReload: Boolean
    ) {
        webViewClient.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        webViewClient.onReceivedSslError(view, handler, error)
    }

    override fun onReceivedClientCertRequest(
        view: WebView?,
        request: ClientCertRequest?
    ) {
        webViewClient.onReceivedClientCertRequest(view, request)
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        webViewClient.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    override fun onUnhandledKeyEvent(
        view: WebView?,
        event: KeyEvent?
    ) {
        webViewClient.onUnhandledKeyEvent(view, event)
    }

    override fun onScaleChanged(
        view: WebView?,
        oldScale: Float,
        newScale: Float
    ) {
        webViewClient.onScaleChanged(view, oldScale, newScale)
    }

    override fun onReceivedLoginRequest(
        view: WebView?,
        realm: String?,
        account: String?,
        args: String?
    ) {
        webViewClient.onReceivedLoginRequest(view, realm, account, args)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?
    ): Boolean {
        return webViewClient.onRenderProcessGone(view, detail)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onSafeBrowsingHit(
        view: WebView?,
        request: WebResourceRequest?,
        threatType: Int,
        callback: SafeBrowsingResponse?
    ) {
        webViewClient.onSafeBrowsingHit(view, request, threatType, callback)
    }
}