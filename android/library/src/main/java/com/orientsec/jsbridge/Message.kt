package com.orientsec.jsbridge

private const val REQUEST_HEAD = "_#JSBRIDGE_REQUEST#"
private const val RESPONSE_HEAD = "_#JSBRIDGE_RESPONSE#"

/**
 * Abstract class Message defines the basic structure and behavior of a message.
 * It provides a serialization method and a deserialization companion object method.
 */
abstract class Message {
    /**
     * Abstract property type, representing the type of the message.
     */
    abstract val type: String

    /**
     * Abstract property map, containing the data of the message.
     */
    internal abstract val map: Map<String, String>

    /**
     * Serializes the message into a string.
     * Converts the message type and content into a specific formatted string, then appends
     * the length of each key-value pair.
     * Format: head{name1:value1,name2:value2,...}[length1,length2,...]
     *
     * @return Serialized string representation of the message.
     */
    fun serialize(): String {
        val sb = StringBuilder(type)
        sb.append('{')
        // Append key-value pairs
        map.entries.forEach {
            sb.append(it.key)
                .append(':')
                .append(it.value)
                .append(',')
        }
        // Remove the last ','
        if (map.isNotEmpty()) sb.deleteAt(sb.length - 1)
        sb.append("}[")
        // Append the length of each key-value pair
        map.map { it.key.length + it.value.length + 1 }
            .forEach { sb.append(it).append(',') }
        // Remove the last ','
        if (map.isNotEmpty()) sb.deleteAt(sb.length - 1)
        sb.append(']')
        return sb.toString()
    }

    companion object {
        /**
         * Companion object provides a static method to deserialize a string into message content.
         * This method first validates the string format, then reconstructs the message content
         * based on the length array and value array.
         *
         * @param msg The string to be deserialized.
         * @param tag The type tag of the message, used to validate the beginning of the string.
         * @return Deserialized message content as a map.
         */
        fun deserialize(msg: String, tag: String): Map<String, String> {
            // Validate tag
            require(msg.startsWith(tag)) { "Invalid tag." }
            val lengthIndex = msg.lastIndexOf('[')
            // Validate length array format
            require(lengthIndex >= 0) { "Invalid length array." }
            require(msg.last() == ']') { "Invalid end symbol of length array." }
            // Read length array
            val lengthArray = try {
                msg.slice(lengthIndex + 1..<msg.lastIndex)
                    .split(',')
                    .map { it.toInt() }
            } catch (e: Exception) {
                throw IllegalArgumentException("Fail to read length array.", e)
            }
            // Validate value array format
            require(msg[tag.length] == '{') { "Invalid start symbol of value array." }
            require(msg[lengthIndex - 1] == '}') { "Invalid end symbol of value array." }
            return try {
                lengthArray
                    .asSequence()
                    // Convert data lengths to ranges
                    .runningFold(0..tag.lastIndex) { acc, it ->
                        acc.last + 2..acc.last + 1 + it
                    }
                    .drop(1)
                    // Validate data splitting
                    .onEachIndexed { index, it ->
                        if (index > 0) require(msg[it.first - 1] == ',') {
                            "Invalid separator at index $index."
                        }
                    }
                    // Key-value pair splitting
                    .map { msg.slice(it) }
                    .map { it.split(':', limit = 2) }
                    .associate { it[0] to it[1] }
            } catch (e: Exception) {
                throw IllegalArgumentException("Fail to read value array.", e)
            }
        }

        /**
         * Creates a message object.
         *
         * Creates and returns a Message object based on the given message string.
         * This method first checks the beginning of the message to determine if it is a request
         * or response, then delegates to the appropriate build method based on the message type.
         *
         * @param msg Message string containing detailed information about the request or response.
         * @return Request or Response object based on the message type.
         * @throws IllegalArgumentException If the message type is unknown, this exception
         * is thrown.
         */
        fun create(msg: String): Message {
            return when {
                msg.startsWith(REQUEST_HEAD) -> Request.build(msg)
                msg.startsWith(RESPONSE_HEAD) -> Response.build(msg)
                else -> throw IllegalArgumentException("Unknown message type.")
            }
        }
    }
}

/**
 * Represents a request from JavaScript to Native.
 *
 * This data class encapsulates the details of a request, including the method name,
 * an optional callback ID, and the data payload.
 *
 * @property name The name of the method being requested.
 * @property callbackId An optional identifier for the callback function in JavaScript.
 * @property data The data payload associated with the request.
 */
data class Request(val name: String, val callbackId: String?, val data: String) :


    Message() {
    override val type: String = REQUEST_HEAD

    override val map: Map<String, String> =
        mapOf("name" to name, "callbackId" to (callbackId ?: ""), "data" to data)

    companion object {
        fun build(str: String): Request {
            val map = deserialize(str, REQUEST_HEAD)
            return Request(
                requireNotNull(map["name"]),
                map["callbackId"],
                requireNotNull(map["data"])
            )
        }
    }
}

/**
 * Represents a response message, inheriting from the Message class.
 *
 * The Response class is primarily used to encapsulate response information,
 * including status codes, messages, callback identifiers, and data.
 *
 * @param code Response status code, default value is 0. Used to indicate the status of
 * the response.
 * @param info Response message, default value is "OK". Provides a brief description of
 * the response.
 * @param callbackId Callback identifier, used to uniquely identify the callback for
 * this response.
 * @param data Response data, containing the specific content of the response.
 */
data class Response(
    val code: Int = 0,
    val info: String = "OK",
    val callbackId: String,
    val data: String
) : Message() {

    override val type: String = RESPONSE_HEAD

    override val map: Map<String, String> = mapOf(
        "code" to code.toString(), "info" to info, "callbackId" to callbackId, "data" to data
    )

    companion object {
        fun build(str: String): Response {
            val array = deserialize(str, RESPONSE_HEAD)
            return Response(
                requireNotNull(array["code"]).toInt(),
                requireNotNull(array["info"]),
                requireNotNull(array["callbackId"]),
                requireNotNull(array["data"])
            )
        }
    }
}