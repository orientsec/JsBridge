/**
 * 消息类型
 */
export type MessageType = 'request' | 'response'

/**
 * 错误码类型
 */
export type ErrorCode = -2 | -1 | 0

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
export class Request {
    /**
     * 消息类型
     */
    readonly type: MessageType = 'request'

    /**
     * 请求方法名
     */
    readonly name: string

    /**
     * 请求数据
     */
    readonly data?: string | null

    /**
     * 回调ID
     */
    readonly callbackId?: string | null

    constructor(name: string, data?: string | null, callbackId?: string | null) {
        this.name = name
        this.data = data
        this.callbackId = callbackId
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
export class Response {
    /**
     * 消息类型
     */
    readonly type: MessageType = 'response'

    /**
     * 响应码
     */
    readonly code: ErrorCode

    /**
     * 响应信息
     */
    readonly info: string

    /**
     * 响应数据
     */
    readonly data?: string | null

    /**
     * 回调ID
     */
    readonly callbackId: string

    constructor(code: ErrorCode, info: string, data: string | null | undefined, callbackId: string) {
        this.code = code
        this.info = info
        this.data = data
        this.callbackId = callbackId
    }
}