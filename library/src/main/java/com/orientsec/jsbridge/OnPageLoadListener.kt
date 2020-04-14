package com.orientsec.jsbridge

/**
 * 页面加载回调。
 */
interface OnPageLoadListener {
    /**
     * 页面加载回调。
     * @param isLoaded 页面是否加载成功。
     */
    fun onPageLoaded(isLoaded: Boolean)
}