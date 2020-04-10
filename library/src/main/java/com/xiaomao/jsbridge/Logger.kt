package com.xiaomao.jsbridge

import android.util.Log

object Logger {
    var debug: Boolean = false

    fun d(tag: String, message: String) {
        if (debug) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        if (debug) {
            Log.i(tag, message)
        }
    }

    fun w(tag: String, message: String) {
        if (debug) {
            Log.w(tag, message)
        }
    }


    fun e(tag: String, message: String) {
        if (debug) {
            Log.e(tag, message)
        }
    }
}