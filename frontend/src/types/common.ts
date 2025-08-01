import { ID, KeyValue } from './base'

// 列表项
export interface Option {
    label: string
    value: ID
    id?: ID
    value1?: string
    key1?: string
    key2?: string
    key3?: string
}

// 分页数据结构
export interface Pagination<T> {
    records: T[]
    pageNo: number
    pageSize: number
    total: number
}

// 选择器值类型
export type SingleValue = string | number | null
export type MultipleValue = ID[]
export type SelectValue = SingleValue | MultipleValue

// 统计数据基础接口
export interface BaseStatsData {
    formattedDate: string
}

// 图表配置基础接口
export interface BaseChartOptions extends KeyValue {
    showTotalTrend?: boolean
}