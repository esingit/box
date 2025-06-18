import mitt, { Emitter, Handler } from 'mitt'

type NotifyPayload = {
  message: string
  type?: 'info' | 'success' | 'error' | 'warning'
}

type Events = {
  'login-error': string
  'auth-state-changed': boolean
  'notify': NotifyPayload
  'login-success': void
  'confirm': {
    title?: string
    message?: string
    confirmText?: string
    cancelText?: string
    type?: 'primary' | 'danger'
    onConfirm?: () => Promise<void> | void
    onCancel?: () => void
  }
}

type ExtendedEmitter = Emitter<Events> & {
  once: <K extends keyof Events>(type: K, handler: Handler<Events[K]>) => () => void
}

const emitter: ExtendedEmitter = mitt<Events>() as ExtendedEmitter

emitter.once = function <K extends keyof Events>(type: K, handler: Handler<Events[K]>) {
  const wrappedHandler: Handler<Events[K]> = (...args) => {
    emitter.off(type, wrappedHandler)
    handler(...args)
  }
  emitter.on(type, wrappedHandler)
  return () => emitter.off(type, wrappedHandler)
}

export default emitter
