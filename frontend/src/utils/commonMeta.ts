import axiosInstance from '@/api/axios'
import { useMetaStore } from '@/store/metaStore'
import { AssetNameRecord } from '@/types/assetName'

const cache = new Map<string | number, { typeName: string; value1: string }>()

/**
 * 根据 ID 获取 CommonMeta 数据（带超时控制 + 缓存）
 */
export async function getCommonMetaById(id: string | number): Promise<{ typeName: string; value1: string } | null> {
    if (!id) return null
    if (cache.has(id)) return cache.get(id)!

    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 5000)

    try {
        const res = await axiosInstance.get(`/api/common-meta/by-id/${id}`, {
            signal: controller.signal
        })
        clearTimeout(timeout)

        if (res.data?.success && res.data.data) {
            cache.set(id, res.data.data)
            return res.data.data
        }
        return null
    } catch {
        clearTimeout(timeout)
        return null
    }
}

/**
 * 清除 CommonMeta 缓存
 */
export function clearCommonMetaCache(): void {
    cache.clear()
}

/**
 * 通用格式化工具
 */
async function formatValue(id: string | number | undefined, fallback?: string): Promise<string> {
    if (!id) return fallback ?? '-'
    const data = await getCommonMetaById(id)
    return data?.value1 || fallback || '-'
}

/**
 * 格式化健身记录中的类型与单位
 */
export async function formatFitnessRecord<
    T extends { assetTypeId: string | number; unitId: string | number; typeValue?: string; unitValue?: string }
>(record: T): Promise<T> {
    if (!record || (record.typeValue && record.unitValue)) return record

    const [typeValue, unitValue] = await Promise.all([
        formatValue(record.assetTypeId, record.typeValue),
        formatValue(record.unitId, record.unitValue)
    ])

    return {
        ...record,
        typeValue,
        unitValue
    }
}

/**
 * 格式化资产记录中的所有元数据
 */
export async function formatAssetRecord<
    T extends {
        assetTypeId: string | number
        unitId: string | number
        assetLocationId: string | number
        assetTypeValue?: string
        unitValue?: string
        assetLocationValue?: string
    } & Record<string, any>
>(record: T): Promise<T> {
    if (!record) return record

    const [assetTypeValue, unitValue, assetLocationValue] = await Promise.all([
        formatValue(record.assetTypeId, record.assetTypeValue),
        formatValue(record.unitId, record.unitValue),
        formatValue(record.assetLocationId, record.assetLocationValue)
    ])

    return {
        ...record,
        assetTypeValue,
        unitValue,
        assetLocationValue
    }
}

/**
 * 判断是否是合法的元数据 ID
 */
function isValidMetaId(value: any): boolean {
    return (
        (typeof value === 'number' && !isNaN(value)) ||
        (typeof value === 'string' && /^\d+$/.test(value))
    )
}

/**
 * 格式化资产名称记录中的所有元数据字段
 */
export async function formatAssetNameRecord(record: AssetNameRecord): Promise<AssetNameRecord> {
    if (!record) return record

    const nameId = isValidMetaId(record.name) ? record.name : undefined
    const descriptionId = isValidMetaId(record.description) ? record.description : undefined

    const [name, description] = await Promise.all([
        formatValue(nameId, String(record.name)),
        formatValue(descriptionId, String(record.description))
    ])

    return {
        ...record,
        name,
        description
    }
}

/**
 * 根据类型设置默认单位
 */
export async function setDefaultUnit(
    typeId: string | number | null,  // 完整的类型定义
    setFieldValue?: (field: string, value: any) => void,
    values?: { unitId?: string | number }
) {
    // 处理各种类型情况
    let actualTypeId: string | number | null = null

    if (Array.isArray(typeId)) {
        // 如果是数组，取第一个有效值
        actualTypeId = typeId.length > 0 ? typeId[0] : null
    } else {
        // 单个值或 null
        actualTypeId = typeId
    }

    // 类型检查和早期返回
    if (!actualTypeId) {
        setFieldValue?.('unitId', '')
        return
    }

    // 确保 typeId 是字符串类型用于比较
    const stringTypeId = String(actualTypeId)

    const metaStore = useMetaStore()

    const fitnessTypes = metaStore.typeMap?.FITNESS_TYPE || []
    const assetTypes = metaStore.typeMap?.ASSET_TYPE || []
    const unitList = metaStore.typeMap?.UNIT || []

    const types = [...fitnessTypes, ...assetTypes]
    const selectedType = types.find(type => String(type.id) === stringTypeId)

    if (!selectedType?.key3) {
        setFieldValue?.('unitId', '')
        return
    }

    const defaultUnit = unitList.find(unit => unit.key1 === selectedType.key3)
    if (!defaultUnit) {
        setFieldValue?.('unitId', '')
        return
    }

    const currentUnitId = values?.unitId
    if (!currentUnitId || String(currentUnitId) !== String(defaultUnit.id)) {
        setFieldValue?.('unitId', defaultUnit.id)
    }
}