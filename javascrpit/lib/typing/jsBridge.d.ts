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
export interface BridgeChannel {
    onMessage(data: string): void;
}
declare global {
    interface Window {
        jsBridge: JsBridge;
        bridgeChannel: BridgeChannel;
        bridgePort: MessagePort;
    }
}
export declare class JsBridge {
    static getInstance(): JsBridge;
    private constructor();
    private receiveMessageQueue?;
    private readonly messageHandlers;
    private readonly responseCallbacks;
    private uniqueId;
    private defaultHandler;
    private postMessage;
    init(handler: MessageHandler): void;
    registerHandler(type: string, handler: MessageHandler): void;
    callHandler(name: string, data: string, handlerCallback?: HandlerCallback): void;
    private handleNativeMessage;
    onMessage(data: string): void;
    private validateParams;
    private onRequest;
    private onResponse;
    private emptyCallback;
    private nativeCallback;
}
