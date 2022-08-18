import { HandlerCallback, JsBridge } from './jsBridge'

interface MessageHandlerFunc {
    (data: string, callback: HandlerCallback): void
}

declare global {
    interface Window {
        WebViewJavascriptBridge: JsBridgeCompat
    }

    interface Event {
        bridge: JsBridgeCompat;
    }
}

export class JsBridgeCompat {
    private jsBridge: JsBridge = JsBridge.getInstance()

    static getInstance(): JsBridgeCompat {
        if (window.WebViewJavascriptBridge == null) {
            window.WebViewJavascriptBridge = new JsBridgeCompat()

            const readyEvent = new Event('WebViewJavascriptBridgeReady')
            readyEvent.bridge = window.WebViewJavascriptBridge
            document.dispatchEvent(readyEvent)
        }
        return window.WebViewJavascriptBridge
    }

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    private constructor() { }

    init(handlerFunc: MessageHandlerFunc): void {
        this.jsBridge.init({
            handle(data, callback) {
                handlerFunc(data, callback)
            }
        })
    }

    registerHandler(type: string, handlerFunc: MessageHandlerFunc): void {
        this.jsBridge.registerHandler(type, {
            handle(data, callback) {
                handlerFunc(data, callback)
            }
        })
    }

    callHandler(type: string, data: string, callback?: (data: string) => void): void {
        if (callback) {
            this.jsBridge.callHandler(type, data, {
                onError: (code: number, info: string) => {
                    callback(info)
                },
                onResult: (result: string) => {
                    callback(result)
                }
            })
        } else {
            this.jsBridge.callHandler(type, data)
        }
    }
}