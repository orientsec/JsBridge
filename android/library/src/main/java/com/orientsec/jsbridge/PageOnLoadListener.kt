package com.orientsec.jsbridge

/**
 * Callback interface for page loading events.
 */
interface PageOnLoadListener {
    /**
     * Called when the page starts loading.
     */
    fun onStart()

    /**
     * Called when the page finishes loading.
     */
    fun onFinish()
}
