/**
 * 消息处理器接口，用于处理来自原生端的消息
 */
export interface MessageHandler {
    /**
     * 处理消息
     * @param data 消息数据
     * @param callback 回调函数
     */
    handle(data: string | null | undefined, callback: HandlerCallback): void;
}
/**
 * 回调函数接口，用于处理消息处理的结果
 */
export interface HandlerCallback {
    /**
     * 成功回调
     * @param data 返回数据
     */
    onResult(data?: string | null): void;
    /**
     * 失败回调
     * @param code 错误码
     * @param info 错误信息
     */
    onError(code: number, info: string): void;
}
/**
 * 桥接通道接口，用于与原生端通信
 */
interface BridgeChannel {
    /**
     * 接收消息
     * @param data 消息数据
     */
    onMessage(data: string): void;
}
/**
 * 全局 Window 接口扩展
 */
declare global {
    interface Window {
        /**
         * JsBridge 实例
         */
        jsBridge: JsBridge;
        /**
         * 桥接通道
         */
        readonly bridgeChannel: BridgeChannel;
        /**
         * 消息端口
         */
        readonly bridgePort: MessagePort;
    }
}
/**
 * JsBridge 类，用于实现 JavaScript 与原生端的通信
 */
export declare class JsBridge {
    /**
     * 单例实例
     */
    private static instance;
    /**
     * 消息处理器映射表
     */
    private readonly messageHandlers;
    /**
     * 回调函数映射表
     */
    private readonly responseCallbacks;
    /**
     * 接收到的请求队列
     */
    private receiveRequestQueue;
    /**
     * 唯一ID生成器
     */
    private uniqueId;
    /**
     * 默认消息处理器
     */
    private defaultHandler?;
    /**
     * 桥接初始化超时时间（毫秒）
     */
    private readonly BRIDGE_INIT_TIMEOUT;
    /**
     * 消息类型常量
     */
    private static readonly MESSAGE_TYPE;
    /**
     * 错误码常量
     */
    private static readonly ERROR_CODE;
    /**
     * 获取 JsBridge 单例实例
     * @returns JsBridge 实例
     */
    static getInstance(): JsBridge;
    /**
     * 私有构造函数，强制使用单例模式
     */
    private constructor();
    /**
     * 处理处理器错误
     * @param error 错误对象
     * @param type 处理器类型
     * @param responseCallback 响应回调
     */
    private handleHandlerError;
    /**
     * 获取消息发送错误信息
     * @param error 错误对象
     * @returns 错误消息
     */
    private handlePostMessageError;
    /**
     * 发送消息到原生端
     * @param data 消息数据
     * @returns 是否发送成功
     */
    private postMessage;
    /**
     * 初始化桥接
     * @returns Promise<void>
     */
    init(): Promise<void>;
    /**
     * 处理初始化请求
     * @param data 初始化数据
     */
    private processInitialRequests;
    /**
     * 解析 JSON 数据
     * @param data JSON 字符串
     * @param context 上下文信息
     * @returns 解析后的数据
     */
    private parseJSON;
    /**
     * 解析初始化数据
     * @param data 初始化数据
     */
    private parseInitData;
    /**
     * 注册消息处理器
     * @param type 消息类型
     * @param handler 消息处理器
     */
    registerHandler(type: string, handler: MessageHandler): void;
    /**
     * 移除消息处理器
     * @param type 消息类型
     * @returns 是否成功移除
     */
    removeHandler(type: string): boolean;
    /**
     * 生成回调ID
     * @returns 回调ID
     */
    private generateCallbackId;
    /**
     * 序列化数据
     * @param data 要序列化的数据
     * @returns 序列化后的字符串
     */
    private serializeData;
    /**
     * 构建请求消息
     * @param name 请求名称
     * @param data 请求数据
     * @param callbackId 回调ID
     * @returns 请求消息
     */
    private buildRequest;
    /**
     * 设置回调函数
     * @param handlerCallback 处理器回调
     * @returns 回调ID
     */
    private setupCallback;
    /**
     * 清理回调函数
     * @param callbackId 回调ID
     * @param handlerCallback 处理器回调
     */
    private cleanupCallback;
    /**
     * 调用处理器
     * @param name 处理器名称
     * @param data 处理数据
     * @param handlerCallback 处理器回调
     */
    callHandler(name: string, data?: string | null, handlerCallback?: HandlerCallback | null): void;
    /**
     * 异步执行任务
     * @param task 要执行的任务
     */
    private runAsync;
    /**
     * 处理原生消息
     * @param name 消息类型
     * @param data 消息数据
     * @param callbackId 回调ID
     */
    private handleNativeMessage;
    /**
     * 类型守卫：判断是否为请求消息
     * @param message 消息对象
     * @returns 是否为请求消息
     */
    private isRequest;
    /**
     * 类型守卫：判断是否为响应消息
     * @param message 消息对象
     * @returns 是否为响应消息
     */
    private isResponse;
    /**
     * 处理接收到的消息
     * @param data 消息数据
     */
    onMessage(data: string): void;
    /**
     * 处理请求消息
     * @param request 请求消息
     */
    private handleRequest;
    /**
     * 处理响应消息
     * @param response 响应消息
     */
    private handleResponse;
    /**
     * 处理回调结果
     * @param callback 回调函数
     * @param response 响应消息
     */
    private handleCallbackResult;
    /**
     * 发送响应消息
     * @param callbackId 回调ID
     * @param code 错误码
     * @param info 错误信息
     * @param data 响应数据
     */
    private sendResponse;
    /**
     * 构建响应消息
     * @param callbackId 回调ID
     * @param code 错误码
     * @param info 错误信息
     * @param data 响应数据
     * @returns 响应消息
     */
    private buildResponse;
    /**
     * 创建回调函数
     * @param onResult 成功回调
     * @param onError 失败回调
     * @returns 回调函数
     */
    private createCallback;
    /**
     * 创建空回调函数
     * @param type 消息类型
     * @returns 空回调函数
     */
    private emptyCallback;
    /**
     * 创建原生回调函数
     * @param callbackId 回调ID
     * @returns 原生回调函数
     */
    private nativeCallback;
}
export {};
