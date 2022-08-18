package com.orientsec.jsbridge

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.webkit.WebView
import kotlin.system.measureTimeMillis

internal fun Context.fixedContext(): Context {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
        return createConfigurationContext(Configuration())
    }
    return this
}