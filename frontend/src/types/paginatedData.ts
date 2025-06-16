/**
 * 分页响应格式
 */
export interface PaginatedData<T> {
    records: T[]
    total: number
    current: number
    size: number
}