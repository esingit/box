// src/types/global.d.ts
import type { MessageApi } from 'naive-ui'

declare global {
    interface Window {
        $message?: MessageApi
    }
}

export {}
