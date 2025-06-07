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

export default emitter