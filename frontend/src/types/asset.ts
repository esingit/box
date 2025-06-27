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
    assetNameId: string | number
    assetTypeId: string | number
    amount: string
    unitId: string
    assetLocationId: string | number
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

export interface BatchAddResult {
    /**
     * 成功处理的记录数
     */
    successCount: number

    /**
     * 总记录数
     */
    totalCount: number

    /**
     * 是否覆盖了现有记录
     */
    overwrote: boolean

    /**
     * 是否复制了历史记录
     */
    copied: boolean

    /**
     * 更新记录数
     */
    updateCount: number

    /**
     * 新增记录数
     */
    addCount: number

    /**
     * 操作结果消息
     */
    message: string
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
    assetNameId: string | number | null
    amount: number | null
    unit?: string
    assetTypeId?: number
    assetLocationId?: number
    [key: string]: any
}