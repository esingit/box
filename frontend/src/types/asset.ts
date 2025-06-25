// 统计数据
export interface StatsData {
    formattedDate: string
    totalAssets: number
    assetsChange: number
    totalLiabilities: number
    liabilitiesChange: number
}

// 查询条件
export interface QueryConditions {
    assetNameIdList: number[]
    assetLocationIdList: number[]
    assetTypeIdList: number[]
    startDate: string
    endDate: string
    remark: string
}

// 标准的资产记录类型
export interface RawAssetRecord {
    id: number | string
    assetNameId: string | number
    assetLocationId: string | number
    assetTypeId: string | number
    unitId: string | number
    amount?: number
    date: string
    remark?: string
    [key: string]: any
}

// 类型定义
export interface AssetRecord {
    id: string
    assetNameId: string
    assetTypeId: string
    amount: string
    unitId: string
    assetLocationId: string
    acquireTime: string
    assetName?: string | null
    assetTypeName?: string | null
    assetTypeValue?: string
    unitName?: string | null
    unitValue?: string
    assetLocationName?: string | null
    assetLocationValue?: string
    remark?: string
    assetNameValue?: string
    formattedAmount?: string
    [key: string]: any
}

// 批量添加结果
export interface BatchAddResult {
    successCount: number
    failedCount?: number
    errors?: string[]
}

// 图表配置
export interface ChartOptionsType {
    showTotalTrend: boolean
    showNameDimension: boolean
    showTypeDimension: boolean
    showLocationDimension: boolean
}

// 图片识别返回的数据格式
export interface RecognizedAssetItem {
    assetName: string
    amount: number
    unit?: string
    assetTypeId?: number
    assetLocationId?: number
    [key: string]: any
}