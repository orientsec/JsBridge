import * as React from 'react'
import * as ReactDOM from 'react-dom'
import type { JsBridgeCompat } from 'js-bridge'

const initBridge = (jsBridge: JsBridgeCompat) => {
    jsBridge.init(
        (data, callback) => {
            console.log(`Default js handler receive request ${data}`)
            callback.onResult('Default js handler: do nothing.')
        }
    )
    jsBridge.registerHandler('hello', (data, callback) => {
        console.log(`Request from native [hello], data: ${data}`)
        callback.onResult('Welcome to call js api [hello]...') 
    })
}

if (window.WebViewJavascriptBridge) {
    initBridge(window.WebViewJavascriptBridge)
} else {
    document.addEventListener('WebViewJavascriptBridgeReady', (event) => {
        initBridge(event.bridge)
    })
}

class JsDemo extends React.Component {
    render() {
        const onclick = () => {
            window.WebViewJavascriptBridge.callHandler('hello', 'Hello native!',
                data => {
                    console.log(`Response from native [hello], data: ${data}`)
                    alert(data)
                })
        }
        return <div onClick={onclick}>调用Native指令(兼容版)</div>
    }
}

ReactDOM.render(<JsDemo />, document.getElementById('jsCompatContainer'))