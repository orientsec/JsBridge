import type { JsBridge } from 'js-bridge'

/**
 * 初始化 JsBridge
 * @param jsBridge JsBridge 实例
 */
const initBridge = async (jsBridge: JsBridge): Promise<void> => {
    try {
        await jsBridge.init()
        console.log('[JsBridgeDemo] Bridge initialized successfully')
    } catch (error) {
        console.error('[JsBridgeDemo] Failed to initialize bridge:', error)
        throw error
    }
}

/**
 * 添加处理器
 */
function addHandler(): void {
    try {
        window.jsBridge.registerHandler('hello', {
            handle: (data, callback) => {
                console.log(`[JsBridgeDemo] Request from native [hello], data: ${data}`)
                callback.onResult('Hello JavaScript!')
            }
        })
        console.log('[JsBridgeDemo] Handler added successfully')
    } catch (error) {
        console.error('[JsBridgeDemo] Failed to add handler:', error)
    }
}

/**
 * 移除处理器
 */
function removeHandler(): void {
    try {
        const removed = window.jsBridge.removeHandler('hello')
        if (removed) {
            console.log('[JsBridgeDemo] Handler removed successfully')
        } else {
            console.warn('[JsBridgeDemo] Handler not found')
        }
    } catch (error) {
        console.error('[JsBridgeDemo] Failed to remove handler:', error)
    }
}

/**
 * 调用原生处理器
 */
function callNativeHandler(): void {
    try {
        window.jsBridge.callHandler(
            'hello',
            'Hello native!',
            {
                onResult(data) {
                    console.log(`[JsBridgeDemo] Response from native [hello], data: ${data}`)
                    if (data) {
                        alert(data)
                    }
                },
                onError(code, info) {
                    console.error(`[JsBridgeDemo] Native handler error: [${code}] ${info}`)
                    alert(`Error: ${info}`)
                }
            }
        )
    } catch (error) {
        console.error('[JsBridgeDemo] Failed to call native handler:', error)
    }
}

/**
 * 设置页面元素事件监听
 */
function setupEventListeners(): void {
    const addButton = document.getElementById('jsbridge-add')
    const removeButton = document.getElementById('jsbridge-remove')
    const callButton = document.getElementById('jsbridge-call')

    if (addButton && removeButton && callButton) {
        addButton.addEventListener('click', addHandler)
        removeButton.addEventListener('click', removeHandler)
        callButton.addEventListener('click', callNativeHandler)
    } else {
        console.warn('[JsBridgeDemo] Some buttons were not found')
    }
}

// 初始化处理
if (window.jsBridge) {
    initBridge(window.jsBridge).catch(() => {
        console.error('[JsBridgeDemo] Failed to initialize bridge')
    })
} else {
    window.addEventListener('WebViewJavascriptBridgeReady', () => {
        initBridge(window.jsBridge).catch(() => {
            console.error('[JsBridgeDemo] Failed to initialize bridge')
        })
    })
}

// 初始化页面
setupEventListeners()