import { JsBridge, MessageHandler, HandlerCallback } from './jsBridge'
import { JsBridgeCompat } from './jsBridgeCompat'

JsBridgeCompat.getInstance()

export { MessageHandler, HandlerCallback, JsBridge, JsBridgeCompat }