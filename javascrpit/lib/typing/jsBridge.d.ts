export interface MessageHandler {
    handle(data: string, callback: HandlerCallback): void;
}
export interface HandlerCallback {
    /**
     * 成功回调。
     */
    onResult(data: string): void;
    /**
     * 失败回调。
     */
    onError(code: number, info: string): void;
}
export interface Bridge {
    request(type: string, data: string, callbackId: string): void;
    response(code: number, info: string, data: string, callbackId: string): void;
}
declare global {
    interface Window {
        jsBridge: JsBridge;
        nativeBridge: Bridge;
    }
}
export declare class JsBridge implements Bridge {
    static getInstance(): JsBridge;
    private constructor();
    private receiveMessageQueue?;
    private readonly messageHandlers;
    private readonly responseCallbacks;
    private uniqueId;
    private defaultHandler;
    init(handler: MessageHandler): void;
    registerHandler(type: string, handler: MessageHandler): void;
    callHandler(type: string, data: string, handlerCallback?: HandlerCallback): void;
    response(code: number, info: string, data: string, callbackId: string): void;
    private handleNativeMessage;
    request(type: string, data: string, callbackId: string): void;
    private emptyCallback;
    private nativeCallback;
}
