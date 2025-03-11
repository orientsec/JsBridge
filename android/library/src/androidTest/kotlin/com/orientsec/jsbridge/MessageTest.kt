package com.orientsec.jsbridge

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageTest {

    @Test
    fun testRequest() {
        val request = Request(
            name = "onReady",
            data = null,
            callbackId = null
        )
        val message = request.toString()
        val req = Request.create(JSONObject(message))
        Assert.assertEquals(request, req)
    }
}