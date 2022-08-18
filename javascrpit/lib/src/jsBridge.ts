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

export interface Bridge {
    request(type: string, data: string, callbackId: string): void
    response(code: number, info: string, data: string, callbackId: string): void
}

class Message {
    readonly type: string

    readonly data: string

    readonly callbackId: string

    constructor(type: string, data: string, callbackId: string) {
        this.type = type
        this.data = data
        this.callbackId = callbackId
    }
}

declare global {
    interface Window {
        jsBridge: JsBridge
        nativeBridge: Bridge
    }
}


export class JsBridge implements Bridge {

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
    }

    private receiveMessageQueue?: Message[] = []

    private readonly messageHandlers: Map<string, MessageHandler> = new Map

    private readonly responseCallbacks: Map<string, HandlerCallback> = new Map

    private uniqueId = 1

    private defaultHandler: MessageHandler | undefined

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
            this.handleNativeMessage(message.type, message.data, message.callbackId)
        }
    }

    // 注册js handler
    registerHandler(type: string, handler: MessageHandler): void {
        this.messageHandlers.set(type, handler)
    }

    // 调用原生handler
    callHandler(type: string, data: string, handlerCallback?: HandlerCallback): void {
        let callbackId: string = ''
        if (handlerCallback) {
            callbackId = 'cb_' + (this.uniqueId++) + '_' + new Date().getTime()
            this.responseCallbacks.set(callbackId, handlerCallback)
        }
        console.info(`JsBridge: call native handler:${type}, callbackId:${callbackId}, data:${data}.`)

        if (window.nativeBridge == undefined) {
            console.error('JsBridge: Native bridge is undefined')
            handlerCallback?.onError(-1, 'Native bridge is undefined.')
            return
        }

        try {
            //调用原生JavascriptInterface
            window.nativeBridge.request(type, JSON.stringify(data), callbackId)
        } catch (e) {
            console.error('JsBridge: excute native JavascriptInterface error.', e)
            this.responseCallbacks.delete(callbackId)
            handlerCallback?.onError(-2, 'Fail to execute native JavascriptInterface.')
        }
    }

    //提供给native调用，用于返回native执行结果。
    response(code: number, info: string, data: string, callbackId: string) {
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

    private handleNativeMessage(type: string, data: string, callbackId: string) {
        setTimeout(() => {
            let responseCallback
            if (callbackId == null || callbackId.trim() === '') {
                responseCallback = this.emptyCallback(type)
            } else {
                responseCallback = this.nativeCallback(type, callbackId)
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

    //提供给native调用js指令。receiveMessageQueue 在会在页面加载完后赋值为null。
    request(type: string, data: string, callbackId: string) {
        console.info(`JsBridge: receive native request:${type}, callbackId:${callbackId}, data:${data}.`)
        if (this.receiveMessageQueue) {
            const message = new Message(type, data, callbackId)
            this.receiveMessageQueue.push(message)
        } else {
            this.handleNativeMessage(type, data, callbackId)
        }
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
    private nativeCallback(type: string, callbackId: string): HandlerCallback {
        const callNative = (code: number, info: string, data?: string) => {
            if (window.nativeBridge == undefined) {
                console.error('JsBridge: native bridge is undefined.')
                return
            }
            if (window.nativeBridge.response == undefined) {
                console.error('JsBridge: native bridge.response() is undefined.')
                return
            }
            try {
                window.nativeBridge.response(code, info, JSON.stringify(data), callbackId)
            } catch (e) {
                console.error(`JsBridge: ${type} response to native error.`, e)
            }
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