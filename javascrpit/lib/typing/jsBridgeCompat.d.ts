import { HandlerCallback } from './jsBridge';
interface MessageHandlerFunc {
    (data: string, callback: HandlerCallback): void;
}
declare global {
    interface Window {
        WebViewJavascriptBridge: JsBridgeCompat;
    }
    interface Event {
        bridge: JsBridgeCompat;
    }
}
export declare class JsBridgeCompat {
    private jsBridge;
    static getInstance(): JsBridgeCompat;
    private constructor();
    init(handlerFunc: MessageHandlerFunc): void;
    registerHandler(type: string, handlerFunc: MessageHandlerFunc): void;
    callHandler(type: string, data: string, callback?: (data: string) => void): void;
}
export {};
