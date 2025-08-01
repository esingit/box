import { ID, BaseRecord, BaseQueryParams } from './base'

// 资产名称查询参数
export interface AssetNameQueryParams extends BaseQueryParams {
    name: string
    description: string
    remark?: string
}

// 资产名称记录
export interface AssetNameRecord extends BaseRecord {
    id: ID
    name: string
    description?: string
}