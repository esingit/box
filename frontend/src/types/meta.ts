// 元数据相关类型定义
import { ID } from './base'

// 元数据查询DTO
export interface CommonMetaQueryDTO {
    typeCode: string
}

// 元数据值对象
export interface CommonMetaVO extends Required<Pick<CommonMetaQueryDTO, 'typeCode'>> {
    id: number
    typeCode: string
    typeName: string
    value: string | number
    label: string
    key1?: string
    key2?: string
    key3?: string
    key4?: string
    value1?: string
    value2?: string
    value3?: string
    value4?: string
}