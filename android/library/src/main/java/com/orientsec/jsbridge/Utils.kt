package com.orientsec.jsbridge

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import org.json.JSONObject

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

/**
 * Returns the value mapped by {@code key} if it exists, coercing it if
 * necessary, or the null if no such mapping exists.
 *
 * The standard JSONObject.getString(key) method will throw a JSONException if the key is not
 * present or if the value associated with the key is not a string. Additionally, if the JSON value
 * is explicitly null, getString may also throw an exception or return the string literal "null".
 *
 * This is not a perfect workaround, but maybe the best approach to get a nullable string from
 * JSONObject.
 *
 */
fun JSONObject.getNullableString(key: String): String? {
    return if (isNull(key)) {
        null
    } else {
        getString(key)
    }
}
