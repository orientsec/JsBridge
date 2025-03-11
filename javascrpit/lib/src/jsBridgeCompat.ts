import { JsBridge } from './jsBridge'
import { logger } from './logger'

/**
 * 简化的回调函数类型
 */
type SimpleCallback = (data?: string | null) => void

/**
 * 消息处理函数类型，与旧版代码兼容
 */
type MessageHandlerFunc = (data: string | undefined | null, callback: SimpleCallback) => void

/**
 * 全局接口扩展
 */
declare global {
    interface Window {
        WebViewJavascriptBridge: JsBridgeCompat
    }

    interface Event {
        bridge: JsBridgeCompat
    }
}

/**
 * JsBridge 兼容层类，提供与旧版本兼容的接口
 */
export class JsBridgeCompat {
    /**
     * JsBridge 实例
     */
    private readonly jsBridge: JsBridge = JsBridge.getInstance()

    /**
     * 获取 JsBridgeCompat 单例实例
     */
    static getInstance(): JsBridgeCompat {
        if (!window.WebViewJavascriptBridge) {
            window.WebViewJavascriptBridge = new JsBridgeCompat()

            const readyEvent = new Event('WebViewJavascriptBridgeReady')
            readyEvent.bridge = window.WebViewJavascriptBridge
            document.dispatchEvent(readyEvent)
        }
        return window.WebViewJavascriptBridge
    }

    /**
     * 私有构造函数，强制使用单例模式
     */
    private constructor() {
        // Private constructor to enforce singleton pattern
    }

    /**
     * 初始化桥接
     */
    async init(): Promise<void> {
        try {
            await this.jsBridge.init()
        } catch (error) {
            logger.error('failed to initialize JsBridge', error)
            throw error
        }
    }

    /**
     * 注册消息处理函数
     * @param type 消息类型
     * @param handlerFunc 处理函数
     */
    registerHandler(type: string, handlerFunc: MessageHandlerFunc): void {
        if (!type || !handlerFunc) {
            throw new Error('Invalid handler registration parameters')
        }

        this.jsBridge.registerHandler(type, {
            handle(data, callback) {
                handlerFunc(data, (result) => {
                    callback.onResult(result)
                })
            }
        })
    }

    /**
     * 移除消息处理函数
     * @param type 消息类型
     * @returns 是否成功移除
     */
    removeHandler(type: string): boolean {
        if (!type) {
            throw new Error('Handler type is required')
        }
        return this.jsBridge.removeHandler(type)
    }

    /**
     * 调用原生处理函数
     * @param type 消息类型
     * @param data 消息数据
     * @param callback 回调函数
     */
    callHandler(type: string, data?: string | null, callback?: SimpleCallback): void {
        if (!type) {
            throw new Error('Handler type is required')
        }

        if (!callback) {
            this.jsBridge.callHandler(type, data)
            return
        }

        this.jsBridge.callHandler(type, data, {
            onResult: callback,
            onError: (code: number, info: string) => callback(info)
        })
    }
}