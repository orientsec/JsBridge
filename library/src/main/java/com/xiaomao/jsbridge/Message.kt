package com.xiaomao.jsbridge

import org.json.JSONObject

interface Message {
    fun toJson(): String
}

data class JSRequest(
    val seqNo: String,
    val handlerName: String,
    val data: String
) : Message {
    override fun toJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("seqNo", seqNo)
        jsonObject.put("handlerName", handlerName)
        jsonObject.put("data", data)
        return jsonObject.toString()
    }
}


data class JSResponse(val seqNo: String, val data: String) : Message {
    override fun toJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("seqNo", seqNo)
        jsonObject.put("data", data)
        return jsonObject.toString()
    }
}