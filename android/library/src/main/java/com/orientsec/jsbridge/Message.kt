package com.orientsec.jsbridge

import org.json.JSONObject

private const val VAL_REQUEST = "request"
private const val VAL_RESPONSE = "response"
private const val KEY_TYPE = "type"
private const val KEY_NAME = "name"
private const val KEY_DATA = "data"
private const val KEY_CALLBACK_ID = "callbackId"
private const val KEY_CODE = "code"
private const val KEY_INFO = "info"

internal interface Message {
    companion object : MessageBuilder {
        private val messageBuilders = mapOf(VAL_REQUEST to Request, VAL_RESPONSE to Response)

        override fun create(jsonObject: JSONObject): Message {
            val type = jsonObject.getString(KEY_TYPE)
            val messageBuilder = messageBuilders[type]
                ?: throw IllegalArgumentException("Unknown message type $type")
            return messageBuilder.create(jsonObject)
        }
    }
}

internal interface MessageBuilder {
    fun create(jsonObject: JSONObject): Message
}

internal data class Request(val name: String, val data: String?, val callbackId: String?) :
    Message {
    override fun toString(): String {
        val jsonObject = JSONObject(
            mapOf(
                KEY_TYPE to VAL_REQUEST,
                KEY_NAME to name,
                KEY_DATA to data,
                KEY_CALLBACK_ID to callbackId
            )
        )
        return jsonObject.toString()
    }

    companion object : MessageBuilder {
        override fun create(jsonObject: JSONObject): Message {
            require(jsonObject.getString(KEY_TYPE) == VAL_REQUEST) { "Not a request message." }
            val name = jsonObject.getString(KEY_NAME)
            val data = jsonObject.getNullableString(KEY_DATA)
            val callbackId = jsonObject.getNullableString(KEY_CALLBACK_ID)
            return Request(name, data, callbackId)
        }
    }
}

internal data class Response(
    val code: Int,
    val info: String,
    val data: String?,
    val callbackId: String
) : Message {

    override fun toString(): String {
        val jsonObject = JSONObject(
            mapOf(
                KEY_TYPE to VAL_RESPONSE,
                KEY_CODE to code,
                KEY_INFO to info,
                KEY_DATA to data,
                KEY_CALLBACK_ID to callbackId
            )
        )
        return jsonObject.toString()
    }

    companion object : MessageBuilder {
        override fun create(jsonObject: JSONObject): Message {
            require(jsonObject.getString(KEY_TYPE) == VAL_RESPONSE) { "Not a response message." }
            val code = jsonObject.getInt(KEY_CODE)
            val info = jsonObject.getString(KEY_INFO)
            val data = jsonObject.getNullableString(KEY_DATA)
            val callbackId = jsonObject.getString(KEY_CALLBACK_ID)
            return Response(code, info, data, callbackId)
        }
    }
}