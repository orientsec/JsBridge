import * as React from 'react'
import * as ReactDOM from 'react-dom'
import type { JsBridge } from 'js-bridge'
// eslint-disable-next-line import/extensions
import('./compat')

console.log(`user-agent: ${navigator.userAgent}`)
var isAndroid = navigator.userAgent.indexOf('Android') > -1
if (!isAndroid) {
    import('js-bridge')
    // eslint-disable-next-line import/extensions
    import('./native')
}

const initBridge = (jsBridge: JsBridge) => {
    jsBridge.init({
        handle(data, callback) {
            console.log(`Default js handler receive request ${data}`)
            callback.onResult('Default js handler: do nothing.')
        }
    })
    jsBridge.registerHandler('hello', {
        handle: (data, callback) => {
            console.log(`Request from native [hello], data: ${data}`)
            callback.onResult('Welcome to call js api [hello]...')
        }
    })
}

if (window.jsBridge) {
    initBridge(window.jsBridge)
} else {
    window.addEventListener('WebViewJavascriptBridgeReady', () => {
        initBridge(window.jsBridge)
    })
}

class JsDemo extends React.Component {
    render() {
        const onclick = () => {
            window.jsBridge.callHandler('hello', 'Hello native!', {
                onError(code, info) {
                    alert(`${code}, ${info}`)
                },
                onResult(data) {
                    console.log(`Response from native [hello], data: ${data}`)
                    alert(data)
                },
            })
        }
        return <div onClick={onclick}>调用Native指令</div>
    }
}

ReactDOM.render(<JsDemo />, document.getElementById('jsContainer'))