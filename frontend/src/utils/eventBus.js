import mitt from 'mitt'

const emitter = mitt()

// 添加 once 方法
emitter.once = function(type, handler) {
  const wrappedHandler = (...args) => {
    emitter.off(type, wrappedHandler)
    handler(...args)
  }
  emitter.on(type, wrappedHandler)
  return () => emitter.off(type, wrappedHandler)
}

// 如果没有监听 notify 事件，消息不会弹出。
emitter.on('notify', () => {})

export default emitter