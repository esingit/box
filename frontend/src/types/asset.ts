import { ID, BaseQueryParams, BaseRecord, KeyValue } from './base'
import { BaseStatsData, BaseChartOptions } from './common'

// 资产统计数据
export interface AssetStatsData extends BaseStatsData {
    totalAssets: number
    assetsChange: number
    totalLiabilities: number
    liabilitiesChange: number
    netAssets: number
    netAssetsChange: number
    investmentAssets: number
    investmentAssetsChange: number
}

// 资产查询条件
export interface AssetQueryConditions extends BaseQueryParams {
    assetNameIdList: number[]
    assetLocationIdList: number[]
    assetTypeIdList: number[]
    remark?: string
}

// 标准的资产记录类型
export interface RawAssetRecord extends BaseRecord {
    assetNameId: ID
    assetLocationId: ID
    assetTypeId: ID
    unitId: ID
    amount?: number
    acquireTime: string
}

// 格式化后的资产记录类型
export interface AssetRecord extends BaseRecord {
    id: ID
    assetNameId: ID
    assetTypeId: ID
    amount: string
    unitId: ID
    assetLocationId: ID
    acquireTime: string
    assetName?: string | null
    assetTypeName?: string | null
    assetTypeValue?: string
    unitName?: string | null
    unitValue?: string
    assetLocationName?: string | null
    assetLocationValue?: string
    assetNameValue?: string
    formattedAmount?: string
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

// 资产图表配置
export interface AssetChartOptions extends BaseChartOptions {
    showNameDimension: boolean
    showTypeDimension: boolean
    showLocationDimension: boolean
}

// 图片识别返回的数据格式
export interface RecognizedAssetItem extends KeyValue {
    assetName: string
    assetNameId: ID | null
    amount: number | null
    unit?: string
    assetTypeId?: ID
    assetLocationId?: ID
}

// OCR识别返回的数据结构
export interface AssetScanImageDTO extends BaseRecord {
    // 基础字段
    assetNameId?: ID
    assetName?: string
    assetTypeId?: ID
    unitId?: ID
    assetLocationId?: ID
    amount?: number
    acquireTime?: string

    // OCR识别相关字段
    originalAssetName?: string
    cleanedAssetName?: string
    matchedAssetName?: string
    matchScore?: number
    isMatched?: boolean

    // 扩展字段
    confidence?: number
    matchedText?: string
    boundingBox?: string
    recognitionTime?: string
}