import mitt, { Emitter, Handler } from 'mitt'

// 通知类型
type ToastType = 'info' | 'success' | 'error' | 'warning'

// 通知参数
type NotifyPayload = {
  message: string
  type?: ToastType
  duration?: number
}

// 确认弹窗参数
type ConfirmPayload = {
  title?: string
  message?: string
  confirmText?: string
  cancelText?: string
  type?: 'primary' | 'danger' // 如需进一步统一风格可改为 ToastType
  onConfirm?: () => Promise<void> | void
  onCancel?: () => void
}

// 定义事件类型
type Events = {
  'login-error': string
  'auth-state-changed': boolean
  'notify': NotifyPayload | string
  'login-success': void
  'confirm': ConfirmPayload
  'show-login': void
  'show-register': void
}

// 扩展 Emitter 类型以支持 once 方法
type ExtendedEmitter = Emitter<Events> & {
  once: <K extends keyof Events>(type: K, handler: Handler<Events[K]>) => () => void
}

// 创建 mitt 实例
const emitter: ExtendedEmitter = mitt<Events>() as ExtendedEmitter

// once 方法实现
emitter.once = function <K extends keyof Events>(type: K, handler: Handler<Events[K]>) {
  const wrappedHandler: Handler<Events[K]> = (...args) => {
    emitter.off(type, wrappedHandler)
    handler(...args)
  }
  emitter.on(type, wrappedHandler)
  return () => emitter.off(type, wrappedHandler)
}

export default emitter
