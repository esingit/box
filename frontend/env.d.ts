/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_API_BASE_URL: string
    readonly VITE_APP_PORT: string
    readonly VITE_APP_TITLE: string
    // 如果还有其他 VITE_ 开头的变量，也在这里声明
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}
