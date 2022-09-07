export interface MessageHandler {
    handle(data: string, callback: HandlerCallback): void
}

export interface HandlerCallback {
    /**
     * 成功回调。
     */
    onResult(data: string): void
    /**
     * 失败回调。
     */
    onError(code: number, info: string): void
}

export interface BridgeChannel {
    onMessage(data: string): void
}

// export declare let bridgeChannel: MessagePort


class Request {

    readonly type: string = 'request'

    readonly name: string

    readonly data: string

    readonly callbackId: string

    constructor(name: string, data: string, callbackId: string) {
        this.name = name
        this.data = data
        this.callbackId = callbackId
    }
}

class Response {
    readonly type: string = 'response'

    readonly code: number

    readonly info: string

    readonly data: string

    readonly callbackId: string

    constructor(code: number, info: string, data: string, callbackId: string) {
        this.code = code
        this.data = data
        this.callbackId = callbackId
        this.info = info
    }
}
declare global {
    interface Window {
        jsBridge: JsBridge
        bridgeChannel: BridgeChannel
        bridgePort: MessagePort
    }
}


export class JsBridge {

    static getInstance(): JsBridge {
        if (!window.jsBridge) {
            console.log('JsBridge: new Javascript bridge.')
            window.jsBridge = new JsBridge

            const readyEvent = new Event('WebViewJavascriptBridgeReady')
            window.dispatchEvent(readyEvent)

        }
        return window.jsBridge
    }

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    private constructor() {
        if (window.bridgePort) {
            window.bridgePort.onmessage = (e) => { this.onMessage(e.data) }
        }
        this.postMessage('JsBridge-Channel-Init')
    }

    private receiveMessageQueue?: Request[] = []

    private readonly messageHandlers: Map<string, MessageHandler> = new Map

    private readonly responseCallbacks: Map<string, HandlerCallback> = new Map

    private uniqueId = 1

    private defaultHandler: MessageHandler | undefined

    //向native发送消息。
    private postMessage(data: string, onError?: (exception: any) => void) {
        try {
            if (window.bridgePort) {
                window.bridgePort.postMessage(data)
            } else if (window.bridgeChannel) {
                window.bridgeChannel.onMessage(data)
            } else {
                console.error('JsBridge: native bridge channel is undefined.')
            }
        } catch (e) {
            if (onError) {
                onError(e)
            }
            console.error(`JsBridge: post message to native error. data: ${data}`, e)
        }
    }

    //set default messageHandler  初始化默认的消息线程
    init(handler: MessageHandler): void {
        if (this.defaultHandler) {
            throw new Error('WebViewJavascriptBridge.init called twice.')
        }

        this.defaultHandler = handler
        const receivedMessages = this.receiveMessageQueue ?? []
        this.receiveMessageQueue = undefined
        for (let i = 0; i < receivedMessages.length; i++) {
            const message = receivedMessages[i]
            this.handleNativeMessage(message.name, message.data, message.callbackId)
        }
    }

    // 注册js handler
    registerHandler(type: string, handler: MessageHandler): void {
        this.messageHandlers.set(type, handler)
    }

    // 调用原生handler
    callHandler(name: string, data: string, handlerCallback?: HandlerCallback): void {
        let callbackId: string = ''
        if (handlerCallback) {
            callbackId = 'cb_' + (this.uniqueId++) + '_' + new Date().getTime()
            this.responseCallbacks.set(callbackId, handlerCallback)
        }
        console.info(`JsBridge: call native handler:${name}, callbackId:${callbackId}, data:${data}.`)

        let request = new Request(name, JSON.stringify(data), callbackId)
        let json = JSON.stringify(request)
        this.postMessage(json, () => {
            this.responseCallbacks.delete(callbackId)
            handlerCallback?.onError(-2, 'Fail to execute native JavascriptInterface.')
        })
    }

    private handleNativeMessage(type: string, data: string, callbackId: string) {
        setTimeout(() => {
            let responseCallback
            if (callbackId == null || callbackId.trim() === '') {
                responseCallback = this.emptyCallback(type)
            } else {
                responseCallback = this.nativeCallback(callbackId)
            }

            //查找指定handler
            const handler: MessageHandler | undefined = this.messageHandlers.get(type) ?? this.defaultHandler

            if (handler == undefined) {
                console.warn(`JsBridge: none js handler for [${type}].`)
                responseCallback.onError(-1, `Handler:${type} not found.`)
                return
            }

            try {
                console.info(`JsBridge: invoke js handler:${type}, data:${data}.`)
                handler.handle(data, responseCallback)
            } catch (e) {
                responseCallback.onError(-2, `Uncaught exception in js handler:${type}.`)
                console.error(`JsBridge: invoke js handler:${type} error.`, e)
            }

        })
    }

    //接收native发送的消息。
    onMessage(data: string) {
        let json = JSON.parse(data)
        let type = json.type
        if (type == 'request') {
            let request = json as Request
            if (this.validateParams(request, 'name', 'data', 'callbackId')) {
                this.onRequest(request.name, request.data, request.callbackId)
            } else {
                console.warn(`JsBridge: required request param lost, ${request}`)
            }
        } else if (type == 'response') {
            let response = json as Response
            if (this.validateParams(response, 'code', 'info', 'data', 'callbackId')) {
                this.onResponse(response.code, response.info, response.data, response.callbackId)
            } else {
                console.warn(`JsBridge: required response param lost, ${response}`)
            }
        } else {
            console.warn(`JsBridge: receive onKnown message: ${data}`)
        }
    }

    private validateParams<T, K extends keyof T>(t: T, ...keys: K[]): boolean {
        keys.forEach(key => {
            if (t[key] === undefined) {
                return false
            }
        })
        return true
    }

    //提供给native调用js指令。receiveMessageQueue 在会在页面加载完后赋值为null。
    private onRequest(name: string, data: string, callbackId: string) {
        console.info(`JsBridge: receive native request:${name}, callbackId:${callbackId}, data:${data}.`)
        if (this.receiveMessageQueue) {
            const message = new Request(name, data, callbackId)
            this.receiveMessageQueue.push(message)
        } else {
            this.handleNativeMessage(name, data, callbackId)
        }
    }

    //提供给native调用，用于返回native执行结果。
    private onResponse(code: number, info: string, data: string, callbackId: string) {
        console.info(`JsBridge: receive native response, callbackId:${callbackId}, code:${code}, info:${info}, data:${data}.`)
        setTimeout(() => {
            const responseCallback = this.responseCallbacks.get(callbackId)
            if (!responseCallback) {
                console.warn(`JsBridge: callback:${callbackId} not found.`)
                return
            }
            this.responseCallbacks.delete(callbackId)
            if (code == 0) {
                responseCallback.onResult(data)
            } else {
                responseCallback.onError(code, info)
            }
        })
    }


    //空回调。
    private emptyCallback(type: string): HandlerCallback {
        const callback = (result: string) => { console.warn(`JsBridge: none callback for [${type}], result:${result}.`) }

        callback.onError = (code: number, info: string) => {
            console.warn(`JsBridge: none callback for [${type}], onError:[${code}, ${info}].`)
        }
        callback.onResult = (data: string) => {
            console.warn(`JsBridge: none callback for [${type}], onResult:${data}.`)
        }
        return callback
    }

    //将js执行结果返回给原生的callback。
    private nativeCallback(callbackId: string): HandlerCallback {
        const callNative = (code: number, info: string, data?: string) => {
            let response = new Response(code, info, JSON.stringify(data), callbackId)
            let json = JSON.stringify(response)
            this.postMessage(json)
        }
        const callback = (result: string) => {
            callNative(0, 'OK', result)
        }
        callback.onError = (code: number, info: string) => {
            callNative(code, info)
        }
        callback.onResult = (data: string) => {
            callNative(0, 'OK', data)
        }
        return callback
    }
}