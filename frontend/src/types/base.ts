// 基础类型定义

// 通用ID类型
export type ID = string | number

// 通用键值对接口
export interface KeyValue {
  [key: string]: any
}

// 通用查询参数基础接口
export interface BaseQueryParams {
  startDate?: string
  endDate?: string
  remark?: string
}

// 通用记录基础接口
export interface BaseRecord extends KeyValue {
  id?: ID
  remark?: string
}