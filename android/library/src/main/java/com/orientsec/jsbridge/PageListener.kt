package com.orientsec.jsbridge

/**
 * 页面加载回调。
 */
interface PageListener {
    /**
     * 开始加载。
     */
    fun onLoadStart()

    /**
     * 页面加载回调。
     */
    fun onLoadFinished()
}