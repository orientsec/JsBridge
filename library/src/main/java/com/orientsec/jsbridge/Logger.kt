package com.orientsec.jsbridge

import android.util.Log
import com.orientsec.jsbridge.JsBridge.Companion.debug

interface Loggable {
    fun debug(message: String)

    fun info(message: String)

    fun warn(message: String)

    fun error(message: String)
}

class Logger(private val tag: String) : Loggable {

    override fun debug(message: String) {
        if (debug) {
            Log.d(tag, message)
        }
    }

    override fun info(message: String) {
        if (debug) {
            Log.i(tag, message)
        }
    }

    override fun warn(message: String) {
        if (debug) {
            Log.w(tag, message)
        }
    }


    override fun error(message: String) {
        if (debug) {
            Log.e(tag, message)
        }
    }
}