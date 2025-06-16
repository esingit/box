/**
 * 通用元数据对象（来自 CommonMeta）
 */
export interface CommonMeta {
    id: number | string
    typeName: string
    value1: string
    [key: string]: any
}