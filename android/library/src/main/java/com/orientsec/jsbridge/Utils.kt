package com.orientsec.jsbridge

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.webkit.WebView
import kotlin.system.measureTimeMillis

/**
 * Returns a fixed context based on the SDK version.
 *
 * For devices running Android Marshmallow (API level 23) or lower, this method creates a new
 * configuration context using an empty [Configuration]. This is necessary to address certain
 * compatibility issues.
 * For devices running Android Nougat (API level 24) or higher, the original context is returned.
 *
 * @return A fixed context suitable for the current SDK version.
 */
internal fun Context.fixedContext(): Context {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
        return createConfigurationContext(Configuration())
    }
    return this
}
