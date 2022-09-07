import * as React from 'react'
import * as ReactDOM from 'react-dom'

class Request {

    readonly type: string = 'request'

    readonly name: string

    readonly data: string

    readonly callbackId: string

    constructor(name: string, data: string, callbackId: string) {
        this.name = name
        this.data = data
        this.callbackId = callbackId
    }
}

class Response {
    readonly type: string = 'response'

    readonly code: number

    readonly info: string

    readonly data: string

    readonly callbackId: string

    constructor(code: number, info: string, data: string, callbackId: string) {
        this.code = code
        this.data = data
        this.callbackId = callbackId
        this.info = info
    }
}

function validateParams<T, K extends keyof T>(t: T, ...keys: K[]): boolean {
    keys.forEach(key => {
        if (t[key] === undefined) {
            return false
        }
    })
    return true
}

let channel = new MessageChannel 

function onRequest(handlerName: string, data: string, callbackId: string) {
    console.log(`request from js, name:${handlerName}, data: ${data}, callbackId: ${callbackId}`)
    let response = new Response(0, 'OK', `Welcome to call native api [${handlerName}]...`, callbackId)
    channel.port2.postMessage(JSON.stringify(response))
}

function onResponse(code: number, info: string, data: string, callbackId: string) {
    console.log(`response from js, code:${code}, info:${info}, data:${data}, callbackId:${callbackId}`)
    alert(data)
}

window.bridgePort = channel.port1
channel.port2.onmessage = (e) => {
    let json = JSON.parse(e.data)
    let type = json.type
    if (type == 'request') {
        let request = json as Request
        if (validateParams(request, 'name', 'data', 'callbackId')) {
            onRequest(request.name, request.data, request.callbackId)
        } else {
            console.warn(`JsBridge: required request param lost, ${request}`)
        }
    } else if (type == 'response') {
        let response = json as Response
        if (validateParams(response, 'code', 'info', 'data', 'callbackId')) {
            onResponse(response.code, response.info, response.data, response.callbackId)
        } else {
            console.warn(`JsBridge: required response param lost, ${response}`)
        }
    } else {
        console.warn(`JsBridge: receive onKnown message: ${e.data}`)
    }
}

class NativeDemo extends React.Component {
    render() {
        const onClick = () => {
            let request = new Request('hello', 'Hello javascript!', 'randomId')
            channel.port2.postMessage(JSON.stringify(request))
        }
        return <div onClick={onClick} >调用Js指令</div>
    }
}

ReactDOM.render(<NativeDemo />, document.getElementById('nativeContainer'))
