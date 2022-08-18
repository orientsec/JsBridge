import * as React from 'react'
import * as ReactDOM from 'react-dom'

window.nativeBridge = {
    request: function (handlerName, data, callbackId) {
        console.log(`request from js, name:${handlerName}, data: ${data}, callbackId: ${callbackId}`)
        window.jsBridge.response(0, 'OK', `Welcome to call native api [${handlerName}]...`, callbackId)
    },
    response: function (code: number, info: string, data: string, callbackId: string) {
        console.log(`response from js, code:${code}, info:${info}, data:${data}, callbackId:${callbackId}`)
        alert(data)
    }
}

class NativeDemo extends React.Component {
    render() {
        const onClick = () => {
            window.jsBridge.request('hello', 'Hello javascript!', 'randomId')
        }
        return <div onClick={onClick} >调用Js指令</div>
    }
}

ReactDOM.render(<NativeDemo />, document.getElementById('nativeContainer'))
