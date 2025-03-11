import { Request, Response, MessageType, ErrorCode } from './message'
import { logger } from './logger'

/**
 * 消息处理器接口，用于处理来自原生端的消息
 */
export interface MessageHandler {
    /**
     * 处理消息
     * @param data 消息数据
     * @param callback 回调函数
     */
    handle(data: string | null | undefined, callback: HandlerCallback): void
}

/**
 * 回调函数接口，用于处理消息处理的结果
 */
export interface HandlerCallback {
    /**
     * 成功回调
     * @param data 返回数据
     */
    onResult(data?: string | null): void

    /**
     * 失败回调
     * @param code 错误码
     * @param info 错误信息
     */
    onError(code: number, info: string): void
}

/**
 * 桥接通道接口，用于与原生端通信
 */
interface BridgeChannel {
    /**
     * 接收消息
     * @param data 消息数据
     */
    onMessage(data: string): void
}

/**
 * 初始化数据接口
 */
interface InitData {
    /**
     * 是否可调试
     */
    debug?: boolean
}

/**
 * 全局 Window 接口扩展
 */
declare global {
    interface Window {
        /**
         * JsBridge 实例
         */
        jsBridge: JsBridge
        /**
         * 桥接通道
         */
        readonly bridgeChannel: BridgeChannel
        /**
         * 消息端口
         */
        readonly bridgePort: MessagePort
    }
}

/**
 * 安全错误类型声明
 */
declare class SecurityError extends Error {
    constructor(message?: string)
}

/**
 * JsBridge 类，用于实现 JavaScript 与原生端的通信
 */
export class JsBridge {
    /**
     * 单例实例
     */
    private static instance = new JsBridge()

    /**
     * 消息处理器映射表
     */
    private readonly messageHandlers: Map<string, MessageHandler> = new Map()

    /**
     * 回调函数映射表
     */
    private readonly responseCallbacks: Map<string, HandlerCallback> = new Map()

    /**
     * 接收到的请求队列
     */
    private receiveRequestQueue: Request[] | undefined = []

    /**
     * 唯一ID生成器
     */
    private uniqueId = 1

    /**
     * 默认消息处理器
     */
    private defaultHandler?: MessageHandler

    /**
     * 桥接初始化超时时间（毫秒）
     */
    private readonly BRIDGE_INIT_TIMEOUT = 5000

    /**
     * 消息类型常量
     */
    private static readonly MESSAGE_TYPE = {
        REQUEST: 'request' as MessageType,
        RESPONSE: 'response' as MessageType
    } as const

    /**
     * 错误码常量
     */
    private static readonly ERROR_CODE = {
        HANDLER_NOT_FOUND: -1 as ErrorCode,
        EXECUTION_FAILED: -2 as ErrorCode,
        SUCCESS: 0 as ErrorCode
    } as const

    /**
     * 获取 JsBridge 单例实例
     * @returns JsBridge 实例
     */
    static getInstance(): JsBridge {
        if (!window.jsBridge) {
            window.jsBridge = JsBridge.instance
            const readyEvent = new Event('WebViewJavascriptBridgeReady')
            window.dispatchEvent(readyEvent)
        }
        return JsBridge.instance
    }

    /**
     * 私有构造函数，强制使用单例模式
     */
    private constructor() {
        // Private constructor to enforce singleton pattern
    }

    /**
     * 处理处理器错误
     * @param error 错误对象
     * @param type 处理器类型
     * @param responseCallback 响应回调
     */
    private handleHandlerError(error: unknown, type: string, responseCallback: HandlerCallback): void {
        let errorMessage
        if (error instanceof TypeError) {
            errorMessage = 'invalid argument type'
        } else if (error instanceof ReferenceError) {
            errorMessage = 'undefined reference'
        } else {
            errorMessage = `${error instanceof Error ? error.message : 'unknown error'}`
        }
        logger.error(`handler failed, name:[${type}], error:[${errorMessage}]`, error)
        responseCallback.onError(JsBridge.ERROR_CODE.EXECUTION_FAILED, errorMessage)
    }

    /**
     * 获取消息发送错误信息
     * @param error 错误对象
     * @returns 错误消息
     */
    private handlePostMessageError(error: unknown) {
        let errorMessage
        if (error instanceof TypeError) {
            errorMessage = 'bridge channel method not available: postMessage or onMessage is not a function'
        } else if (error instanceof SecurityError) {
            errorMessage = 'security error: cross-origin communication not allowed'
        } else {
            errorMessage = `unexpected error while posting message: 
            ${error instanceof Error ? error.message : 'Unknown error'}`
        }
        logger.error(errorMessage, error)
    }

    /**
     * 发送消息到原生端
     * @param data 消息数据
     * @returns 是否发送成功
     */
    private postMessage(data: string): boolean {
        try {
            if (window.bridgePort) {
                logger.info(`messagePort send message: ${data}`)
                window.bridgePort.postMessage(data)
                return true
            } else if (window.bridgeChannel) {
                logger.info(`bridgeChannel send message: ${data}`)
                window.bridgeChannel.onMessage(data)
                return true
            }
            logger.error('bridge channel not available: neither messagePort nor bridgeChannel is defined')
            return false
        } catch (error) {
            this.handlePostMessageError(error)
            return false
        }
    }

    /**
     * 初始化桥接
     * @returns Promise<void>
     */
    init(): Promise<void> {
        return new Promise((resolve, reject) => {
            if (this.defaultHandler) {
                reject(new Error('WebViewJavascriptBridge.init called twice.'))
                return
            }

            if (window.bridgePort) {
                window.bridgePort.onmessage = (e) => this.onMessage(e.data)
            }

            const timeoutId = setTimeout(() => {
                reject(new Error('Bridge initialization timeout'))
            }, this.BRIDGE_INIT_TIMEOUT)

            this.callHandler('_JS_BRIDGE_INIT', '', {
                onError: (code: number, info: string) => {
                    clearTimeout(timeoutId)
                    reject(new Error(`Bridge init failed: ${info} (${code})`))
                },
                onResult: (data) => {
                    clearTimeout(timeoutId)
                    this.processInitialRequests(data)
                    resolve()
                }
            })
        })
    }

    /**
     * 处理初始化请求
     * @param data 初始化数据
     */
    private processInitialRequests(data: string | null | undefined): void {
        const receivedRequests = this.receiveRequestQueue ?? []
        this.receiveRequestQueue = undefined

        receivedRequests.forEach(request => {
            this.handleNativeMessage(request.name, request.data, request.callbackId)
        })

        if (data) {
            this.parseInitData(data)
        }
    }

    /**
     * 解析 JSON 数据
     * @param data JSON 字符串
     * @param context 上下文信息
     * @returns 解析后的数据
     */
    private parseJSON<T>(data: string, context: string): T | null {
        try {
            return JSON.parse(data) as T
        } catch (error) {
            logger.error(`failed to parse ${context}`, error)
            return null
        }
    }

    /**
     * 解析初始化数据
     * @param data 初始化数据
     */
    private parseInitData(data: string): void {
        const initData = this.parseJSON<InitData>(data, 'init data')
        if (initData && typeof initData.debug === 'boolean') {
            logger.debuggable = initData.debug
        }
    }

    /**
     * 注册消息处理器
     * @param type 消息类型
     * @param handler 消息处理器
     */
    registerHandler(type: string, handler: MessageHandler): void {
        if (!type || !handler) {
            throw new Error('Invalid handler registration parameters')
        }
        this.messageHandlers.set(type, handler)
    }

    /**
     * 移除消息处理器
     * @param type 消息类型
     * @returns 是否成功移除
     */
    removeHandler(type: string): boolean {
        if (!type) {
            throw new Error('Handler type is required')
        }
        return this.messageHandlers.delete(type)
    }

    /**
     * 生成回调ID
     * @returns 回调ID
     */
    private generateCallbackId(): string {
        return `cb_${this.uniqueId++}_${Date.now()}`
    }

    /**
     * 序列化数据
     * @param data 要序列化的数据
     * @returns 序列化后的字符串
     */
    private serializeData(data: unknown): string | null {
        try {
            return JSON.stringify(data)
        } catch (error) {
            logger.error('railed to serialize data', error)
            return null
        }
    }

    /**
     * 构建请求消息
     * @param name 请求名称
     * @param data 请求数据
     * @param callbackId 回调ID
     * @returns 请求消息
     */
    private buildRequest(name: string, data: string | null | undefined, callbackId: string | undefined): Request {
        return new Request(name, this.serializeData(data), callbackId)
    }

    /**
     * 设置回调函数
     * @param handlerCallback 处理器回调
     * @returns 回调ID
     */
    private setupCallback(handlerCallback: HandlerCallback | null): string | undefined {
        if (!handlerCallback) {
            return undefined
        }

        const callbackId = this.generateCallbackId()
        this.responseCallbacks.set(callbackId, handlerCallback)
        return callbackId
    }

    /**
     * 清理回调函数
     * @param callbackId 回调ID
     * @param handlerCallback 处理器回调
     */
    private cleanupCallback(callbackId: string, handlerCallback: HandlerCallback): void {
        this.responseCallbacks.delete(callbackId)
        handlerCallback.onError(JsBridge.ERROR_CODE.EXECUTION_FAILED, 'Failed to execute native JavascriptInterface')
    }

    /**
     * 调用处理器
     * @param name 处理器名称
     * @param data 处理数据
     * @param handlerCallback 处理器回调
     */
    callHandler(name: string, data?: string | null, handlerCallback?: HandlerCallback | null): void {
        const callbackId = this.setupCallback(handlerCallback ?? null)

        logger.info(`call native handler name:[${name}], callbackId:[${callbackId}], data:[${data}]`)

        const request = this.buildRequest(name, data, callbackId)
        const success = this.postMessage(JSON.stringify(request))
        if (!success && handlerCallback && callbackId) {
            this.cleanupCallback(callbackId, handlerCallback)
        }
    }

    /**
     * 异步执行任务
     * @param task 要执行的任务
     */
    private runAsync(task: () => void): void {
        queueMicrotask(task)
    }

    /**
     * 处理原生消息
     * @param name 消息类型
     * @param data 消息数据
     * @param callbackId 回调ID
     */
    private handleNativeMessage(name: string, data?: string | null, callbackId?: string | null): void {
        this.runAsync(() => {
            const responseCallback = callbackId ?
                this.nativeCallback(callbackId) :
                this.emptyCallback(name)

            const handler = this.messageHandlers.get(name) ?? this.defaultHandler

            if (!handler) {
                logger.warn(`no handler for [${name}]`)
                responseCallback.onError(JsBridge.ERROR_CODE.HANDLER_NOT_FOUND, `handler [${name}] not found`)
                return
            }

            try {
                logger.info(`invoke js handler:[${name}], data:[${data}]`)
                handler.handle(data, responseCallback)
            } catch (error) {
                this.handleHandlerError(error, name, responseCallback)
            }
        })
    }

    /**
     * 类型守卫：判断是否为请求消息
     * @param message 消息对象
     * @returns 是否为请求消息
     */
    private isRequest(message: unknown): message is Request {
        const msg = message as { type?: MessageType; name?: string }
        return Boolean(msg && typeof msg === 'object'
                           && msg.type === JsBridge.MESSAGE_TYPE.REQUEST
                           && typeof msg.name === 'string')
    }

    /**
     * 类型守卫：判断是否为响应消息
     * @param message 消息对象
     * @returns 是否为响应消息
     */
    private isResponse(message: unknown): message is Response {
        const msg = message as { type?: MessageType; callbackId?: string }
        return Boolean(msg && typeof msg === 'object'
                           && msg.type === JsBridge.MESSAGE_TYPE.RESPONSE
                           && typeof msg.callbackId === 'string')
    }

    /**
     * 处理接收到的消息
     * @param data 消息数据
     */
    onMessage(data: string): void {
        const message = this.parseJSON(data, 'message')
        if (!message) {
            return
        }

        if (this.isRequest(message)) {
            this.handleRequest(message as Request)
        } else if (this.isResponse(message)) {
            this.handleResponse(message as Response)
        } else {
            logger.warn(`unknown message type:[${data}]`)
        }
    }

    /**
     * 处理请求消息
     * @param request 请求消息
     */
    private handleRequest(request: Request): void {
        if (!request.name) {
            logger.warn('invalid request, missing name')
            return
        }

        if (this.receiveRequestQueue) {
            this.receiveRequestQueue.push(request)
            return
        }
        this.handleNativeMessage(request.name, request.data, request.callbackId)
    }

    /**
     * 处理响应消息
     * @param response 响应消息
     */
    private handleResponse(response: Response): void {
        if (!response.callbackId) {
            logger.warn('invalid response, missing callbackId')
            return
        }

        this.runAsync(() => {
            const callback = this.responseCallbacks.get(response.callbackId)
            if (!callback) {
                logger.warn(`callback id:[${response.callbackId}] not found`)
                return
            }

            this.responseCallbacks.delete(response.callbackId)
            this.handleCallbackResult(callback, response)
        })
    }

    /**
     * 处理回调结果
     * @param callback 回调函数
     * @param response 响应消息
     */
    private handleCallbackResult(callback: HandlerCallback, response: Response): void {
        const { code, data, info } = response
        if (code === JsBridge.ERROR_CODE.SUCCESS) {
            callback.onResult(data)
            return
        }
        callback.onError(code, info)
    }

    /**
     * 发送响应消息
     * @param callbackId 回调ID
     * @param code 错误码
     * @param info 错误信息
     * @param data 响应数据
     */
    private sendResponse(callbackId: string, code: ErrorCode, info: string, data?: string | null): void {
        const response = this.buildResponse(callbackId, code, info, data)
        const success = this.postMessage(JSON.stringify(response))
        if (!success) {
            logger.error(`failed to send response for callbackId:[${callbackId}]`)
        }
    }

    /**
     * 构建响应消息
     * @param callbackId 回调ID
     * @param code 错误码
     * @param info 错误信息
     * @param data 响应数据
     * @returns 响应消息
     */
    private buildResponse(callbackId: string, code: ErrorCode, info: string, data?: string | null): Response {
        return new Response(code, info, this.serializeData(data), callbackId)
    }

    /**
     * 创建回调函数
     * @param onResult 成功回调
     * @param onError 失败回调
     * @returns 回调函数
     */
    private createCallback(onResult: (data?: string | null) => void, onError: (code: number, info: string) => void)
        : HandlerCallback {
        return Object.assign(
            (result?: string | null) => onResult(result),
            { onResult, onError }
        )
    }

    /**
     * 创建空回调函数
     * @param type 消息类型
     * @returns 空回调函数
     */
    private emptyCallback(type: string): HandlerCallback {
        return this.createCallback(
            (result) => logger.warn(`no callback for [${type}], result:${result}`),
            (code, info) => logger.warn(`no callback for [${type}], error:[${code}, ${info}]`)
        )
    }

    /**
     * 创建原生回调函数
     * @param callbackId 回调ID
     * @returns 原生回调函数
     */
    private nativeCallback(callbackId: string): HandlerCallback {
        return this.createCallback(
            (data) => this.sendResponse(callbackId, JsBridge.ERROR_CODE.SUCCESS, 'OK', data),
            (code, info) => this.sendResponse(callbackId, code as ErrorCode, info)
        )
    }
}