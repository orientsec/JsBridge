import * as React from 'react'
import * as ReactDOM from 'react-dom'

let port: MessagePort

function registerChannel(me: MessageEvent) {
    console.log('-----register channel-----')
    port = me.ports[0]
    port.onmessage = (e) => {
        console.log(e.data)
    }
}


window.addEventListener('message', e => {
    // console.log(e)
    console.log(`message event => ${e.data}`)
    if (e.data == 'Android bridge channel.') {
        registerChannel(e)
    }
})


declare let jsBridge: Window

jsBridge.onmessage = e => {
    console.log(e.data)
}

class JsDemo extends React.Component {
    render() {
        const onclick = () => {
            jsBridge.postMessage('message from h5 channel')
        }
        return <div onClick={onclick}>发送消息</div>
    }
}

ReactDOM.render(<JsDemo />, document.getElementById('jsContainer'))