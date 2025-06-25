// 🔥 类型定义
export interface QueryParams {
    name: string
    description: string
    remark: string
}

export interface AssetNameRecord {
    id: number | string
    name: string
    description?: string
    remark?: string
    [key: string]: any
}