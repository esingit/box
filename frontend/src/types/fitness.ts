// 健身记录表单数据，用于新增和编辑
export interface FitnessFormData {
    typeId: string
    count: string | number
    unitId: string
    finishTime: string
    remark?: string
}

// 查询条件
export interface QueryConditions {
    typeIdList: number[]
    startDate: string
    endDate: string
    remark: string
}

// 统计数据
export interface StatsData {
    monthlyCount: number
    weeklyCount: number
    lastWorkoutDays: number
    nextWorkoutDay: string
    proteinIntake: number
    carbsIntake: number
}

// 健身记录项（列表中的每一条记录）
export interface FitnessRecord {
    id: number
    typeValue?: string
    count: number
    unitValue?: string
    finishTime: string
}

// 类型定义
export interface RawFitnessRecord {
    id: number | string
    assetTypeId: string | number
    unitId: string | number
    date: string
    duration?: number
    remark?: string
    [key: string]: any
}

// 类型定义
export interface FormattedFitnessRecord {
    id: number | string
    assetTypeId?: string | number
    unitId?: string | number
    typeId?: number | string
    typeName?: string
    typeValue?: string
    unitValue?: string
    count?: string | number
    finishTime?: string
    date?: string
    duration?: number
    remark?: string
    [key: string]: any
}