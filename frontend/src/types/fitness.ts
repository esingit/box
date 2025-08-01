import { ID, BaseQueryParams, BaseRecord } from './base'
import { BaseStatsData } from './common'

// 健身记录表单数据，用于新增和编辑
export interface FitnessFormData {
    typeId: ID
    count: string | number
    unitId: ID
    finishTime: string
    remark?: string
}

// 健身查询条件
export interface FitnessQueryConditions extends BaseQueryParams {
    typeIdList: number[]
    remark?: string
}

// 健身统计数据
export interface FitnessStatsData extends BaseStatsData {
    monthlyCount: number
    weeklyCount: number
    lastWorkoutDays: number
    nextWorkoutDay: string
    proteinIntake: number
    carbsIntake: number
}

// 健身记录项（列表中的每一条记录）
export interface FitnessRecord extends BaseRecord {
    id: ID
    typeValue?: string
    count: number
    unitValue?: string
    finishTime: string
}

// 原始健身记录
export interface RawFitnessRecord extends BaseRecord {
    id: ID
    assetTypeId: ID
    unitId: ID
    date: string
    duration?: number
}

// 格式化后的健身记录
export interface FormattedFitnessRecord extends BaseRecord {
    id: ID
    assetTypeId?: ID
    unitId?: ID
    typeId?: ID
    typeName?: string
    typeValue?: string
    unitValue?: string
    count?: string | number
    finishTime?: string
    date?: string
    duration?: number
}