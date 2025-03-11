/**
 * 简化的回调函数类型
 */
declare type SimpleCallback = (data?: string | null) => void;
/**
 * 消息处理函数类型，与旧版代码兼容
 */
declare type MessageHandlerFunc = (data: string | undefined | null, callback: SimpleCallback) => void;
/**
 * 全局接口扩展
 */
declare global {
    interface Window {
        WebViewJavascriptBridge: JsBridgeCompat;
    }
    interface Event {
        bridge: JsBridgeCompat;
    }
}
/**
 * JsBridge 兼容层类，提供与旧版本兼容的接口
 */
export declare class JsBridgeCompat {
    /**
     * JsBridge 实例
     */
    private readonly jsBridge;
    /**
     * 获取 JsBridgeCompat 单例实例
     */
    static getInstance(): JsBridgeCompat;
    /**
     * 私有构造函数，强制使用单例模式
     */
    private constructor();
    /**
     * 初始化桥接
     */
    init(): Promise<void>;
    /**
     * 注册消息处理函数
     * @param type 消息类型
     * @param handlerFunc 处理函数
     */
    registerHandler(type: string, handlerFunc: MessageHandlerFunc): void;
    /**
     * 移除消息处理函数
     * @param type 消息类型
     * @returns 是否成功移除
     */
    removeHandler(type: string): boolean;
    /**
     * 调用原生处理函数
     * @param type 消息类型
     * @param data 消息数据
     * @param callback 回调函数
     */
    callHandler(type: string, data?: string | null, callback?: SimpleCallback): void;
}
export {};
