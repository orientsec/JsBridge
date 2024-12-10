package com.orientsec.jsbridge

/**
 * Native handler.
 */
fun interface BridgeHandler {
    /**
     * Handle js request.
     * @param data request data.
     * @param callback response callback.
     */
    fun handle(data: String, callback: BridgeCallback)
}

/**
 * Defines a callback interface for handling the results of bridge operations.
 * This interface includes two methods for handling success and error callbacks.
 */
interface BridgeCallback {
    /**
     * Success callback method.
     * This method is called when the bridge operation is successful,
     * and it returns the success data.
     *
     * @param data The success data, containing relevant information after the operation succeeds.
     */
    fun onSuccess(data: String)

    /**
     * Error callback method.
     * This method is called when the bridge operation fails, and it returns the error code
     * and error information.
     *
     * @param code The error code, indicating the type or reason for the failure.
     * @param info The error information, providing a more detailed description of the failure.
     */
    fun onError(code: Int, info: String)
}
