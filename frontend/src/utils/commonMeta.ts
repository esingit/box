import axiosInstance from '@/utils/axios'
import axios from 'axios'
import {useMetaStore} from '@/store/metaStore'

// 缓存已获取的数据
const cache = new Map<string | number, { typeName: string; value1: string }>()

/**
 * 根据ID获取 CommonMeta 的值
 */
export async function getCommonMetaById(id: string | number): Promise<{ typeName: string; value1: string } | null> {
    if (!id) {
        console.debug('[CommonMeta] ID为空')
        return null
    }

    if (cache.has(id)) {
        const cached = cache.get(id)!
        console.debug(`[CommonMeta] 命中缓存: ${id}`, cached)
        return cached
    }

    try {
        console.debug(`[CommonMeta] 请求ID: ${id}`)
        const res = await axiosInstance.get(`/api/common-meta/by-id/${id}`, {
            signal: AbortSignal.timeout(5000)
        })

        if (res.data?.success && res.data.data) {
            cache.set(id, res.data.data)
            console.debug(`[CommonMeta] 成功获取: ${id}`, res.data.data)
            return res.data.data
        }

        console.warn(`[CommonMeta] 数据为空或失败: ${id}`, res.data)
        return null
    } catch (err: any) {
        if (axios.isCancel(err)) {
            console.debug(`[CommonMeta] 请求取消: ${id}`)
        } else {
            console.error(`[CommonMeta] 请求失败: ${id}`, err)
        }
        return null
    }
}

/**
 * 清除 CommonMeta 缓存
 */
export function clearCommonMetaCache(): void {
    console.debug('[CommonMeta] 缓存已清除')
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
    if (!record) {
        console.warn('[Asset] 记录为空')
        return record
    }

    console.debug('[Asset] 格式化记录:', record)

    const [assetTypeValue, unitValue, assetLocationValue] = await Promise.all([
        formatValue(record.assetTypeId, record.assetTypeValue),
        formatValue(record.unitId, record.unitValue),
        formatValue(record.assetLocationId, record.assetLocationValue)
    ])

    const formatted = {
        ...record,
        assetTypeValue,
        unitValue,
        assetLocationValue
    }

    console.debug('[Asset] 格式化完成:', formatted)
    return formatted
}

/**
 * 格式化资产名称记录中的所有元数据
 */
export async function formatAssetNameRecord<
    T extends {
        name: string | number
        description: string | number
    } & Record<string, any>
>(record: T): Promise<T> {
    if (!record) {
        console.warn('[AssetName] 记录为空')
        return record
    }

    console.debug('[AssetName] 格式化记录:', record)

    const [name, description] = await Promise.all([
        formatValue(String(record.name), String(record.name)),
        formatValue(String(record.description), String(record.description))
    ])

    const formatted = {
        ...record,
        name,
        description
    }

    console.debug('[AssetName] 格式化完成:', formatted)
    return formatted
}

export async function setDefaultUnit(
    typeId: string,
    setFieldValue?: (field: string, value: any) => void,
    values?: { unitId?: string | number }
) {
    const metaStore = useMetaStore()

    const fitnessTypes = metaStore.typeMap?.FITNESS_TYPE || []
    const assetTypes = metaStore.typeMap?.ASSET_TYPE || []
    const unitList = metaStore.typeMap?.UNIT || []

    const types = [...fitnessTypes, ...assetTypes]
    const selectedType = types.find(type => String(type.id) === String(typeId))

    if (!selectedType?.key3) {
        if (typeof setFieldValue === 'function') {
            setFieldValue('unitId', '')
        }
        return
    }

    const defaultUnit = unitList.find(unit => unit.key1 === selectedType.key3)
    if (!defaultUnit) return

    const currentUnitId = values?.unitId

    if (!currentUnitId || String(currentUnitId) !== String(defaultUnit.id)) {
        if (typeof setFieldValue === 'function') {
            setFieldValue('unitId', defaultUnit.id)
        }
    }
}
