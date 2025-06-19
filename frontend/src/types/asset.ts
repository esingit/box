/**
 * 资产记录的原始结构（从后端获取的）
 */
export interface AssetRecordRaw {
    id: number | string
    assetNameId: number | string
    assetTypeId: number | string
    unitId: number | string
    assetLocationId: number | string
    amount: number
    remark?: string
    date: string
    [key: string]: any
}

/**
 * 资产记录的格式化结构（前端展示用）
 */
export interface AssetRecord extends AssetRecordRaw {
    assetTypeValue?: string
    unitValue?: string
    locationValue?: string
}

/**
 * 查询资产记录的参数
 */
export interface AssetQueryParams {
    page?: number
    pageSize?: number
    assetNameId?: number | string
    assetLocationId?: number | string
    assetTypeId?: number | string
    startDate?: string
    endDate?: string
    remark?: string
}