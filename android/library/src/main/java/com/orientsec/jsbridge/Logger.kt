package com.orientsec.jsbridge

import android.util.Log

interface Loggable {
    fun debug(message: String)

    fun info(message: String)

    fun warn(message: String)

    fun error(message: String)

    fun error(message: String, throwable: Throwable)
}

internal val BridgeLogger = Logger("JsBridge")

class Logger(private val tag: String) : Loggable {
    private fun String.fillUp(): String {
        return "$this    [Thread: ${Thread.currentThread().name}]"
    }

    override fun debug(message: String) {
        if (debug) {
            Log.d(tag, message.fillUp())
        }
    }

    override fun info(message: String) {
        if (debug) {
            Log.i(tag, message.fillUp())
        }
    }

    override fun warn(message: String) {
        if (debug) {
            Log.w(tag, message.fillUp())
        }
    }


    override fun error(message: String) {
        if (debug) {
            Log.e(tag, message.fillUp())
        }
    }

    override fun error(message: String, throwable: Throwable) {
        if (debug) {
            Log.e(tag, message.fillUp(), throwable)
        }
    }
}