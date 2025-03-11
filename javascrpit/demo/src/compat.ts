import type { JsBridgeCompat } from 'js-bridge'

/**
 * 初始化 JsBridge
 * @param jsBridge JsBridgeCompat 实例
 */
const initBridge = async (jsBridge: JsBridgeCompat): Promise<void> => {
    try {
        await jsBridge.init()
        console.log('[JsBridgeCompatDemo] Bridge initialized successfully')
    } catch (error) {
        console.error('[JsBridgeCompatDemo] Failed to initialize bridge:', error)
        throw error
    }
}

/**
 * 添加处理器
 */
function addHandler(): void {
    try {
        window.WebViewJavascriptBridge.registerHandler('hello', (data, callback) => {
            console.log(`[JsBridgeCompatDemo] Request from native [hello], data: ${data}`)
            callback('Hello JavaScript!')
        })
        console.log('[JsBridgeCompatDemo] Handler added successfully')
    } catch (error) {
        console.error('[JsBridgeCompatDemo] Failed to add handler:', error)
    }
}

/**
 * 移除处理器
 */
function removeHandler(): void {
    try {
        const removed = window.WebViewJavascriptBridge.removeHandler('hello')
        if (removed) {
            console.log('[JsBridgeCompatDemo] Handler removed successfully')
        } else {
            console.warn('[JsBridgeCompatDemo] Handler not found')
        }
    } catch (error) {
        console.error('[JsBridgeCompatDemo] Failed to remove handler:', error)
    }
}

/**
 * 调用原生处理器
 */
function callNativeHandler(): void {
    try {
        window.WebViewJavascriptBridge.callHandler(
            'hello',
            'Hello native!',
            (data) => {
                console.log(`[JsBridgeCompatDemo] Response from native [hello], data: ${data}`)
                if (data) {
                    alert(data)
                }
            }
        )
    } catch (error) {
        console.error('[JsBridgeCompatDemo] Failed to call native handler:', error)
    }
}

/**
 * 设置页面元素事件监听
 */
function setupEventListeners(): void {
    const addButton = document.getElementById('jsbridgecompat-add')
    const removeButton = document.getElementById('jsbridgecompat-remove')
    const callButton = document.getElementById('jsbridgecompat-call')

    if (addButton && removeButton && callButton) {
        addButton.addEventListener('click', addHandler)
        removeButton.addEventListener('click', removeHandler)
        callButton.addEventListener('click', callNativeHandler)
    } else {
        console.warn('[JsBridgeCompatDemo] Some buttons were not found')
    }
}

// 初始化处理
if (window.WebViewJavascriptBridge) {
    initBridge(window.WebViewJavascriptBridge).catch(() => {
        console.error('[JsBridgeCompatDemo] Failed to initialize bridge')
    })
} else {
    document.addEventListener('WebViewJavascriptBridgeReady', (event: Event) => {
        const customEvent = event as Event & { bridge: JsBridgeCompat }
        initBridge(customEvent.bridge).catch(() => {
            console.error('[JsBridgeCompatDemo] Failed to initialize bridge')
        })
    })
}

// 初始化页面
setupEventListeners()