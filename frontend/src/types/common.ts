// 列表项
export interface Option {
    label: string
    value: string | number
    id?: string | number
    value1?: string
    key1?: string
    key2?: string
    key3?: string
}

export interface Pagination<T> {
    records: T[]
    pageNo: number
    pageSize: number
    total: number
}

export type SingleValue = string | number | null
export type MultipleValue = (string | number)[]
export type SelectValue = SingleValue | MultipleValue