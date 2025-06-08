import axios from './axios'

// 缓存已获取的数据
const cache = new Map()

/**
 * 根据ID获取CommonMeta的值
 * @param {string|number} id CommonMeta的ID
 * @returns {Promise<{typeName: string, value1: string}|null>} 返回对应的CommonMeta数据
 */
export async function getCommonMetaById(id) {
    if (!id) {
        console.debug('getCommonMetaById: ID为空')
        return null
    }
    
    // 检查缓存
    if (cache.has(id)) {
        console.debug(`getCommonMetaById: 从缓存获取到ID ${id} 的数据:`, cache.get(id))
        return cache.get(id)
    }

    try {
        console.debug(`getCommonMetaById: 正在从服务器获取ID ${id} 的数据...`)
        const res = await axios.get(`/api/common-meta/by-id/${id}`, {
            // 设置取消请求的标识符
            signal: AbortSignal.timeout(5000), // 5秒超时
            // 允许重复请求
            allowDuplicate: true
        })
        
        if (res.data?.success) {
            console.debug(`getCommonMetaById: 成功获取ID ${id} 的数据:`, res.data.data)
            if (res.data.data) {
                cache.set(id, res.data.data)
                return res.data.data
            }
        }
        console.debug(`getCommonMetaById: 获取ID ${id} 的数据失败，服务器返回:`, res.data)
        return null
    } catch (error) {
        if (axios.isCancel(error)) {
            console.debug(`getCommonMetaById: 请求已取消 [ID=${id}]`)
            return null
        }
        console.error('获取CommonMeta数据失败:', error)
        return null
    }
}

/**
 * 清除缓存数据
 */
export function clearCommonMetaCache() {
    console.debug('clearCommonMetaCache: 正在清除缓存...')
    cache.clear()
}

/**
 * 格式化健身记录中的类型和单位
 * @param {Object} record 健身记录数据
 * @returns {Promise<Object>} 格式化后的记录
 */
export async function formatFitnessRecord(record) {
    // 如果记录中已经有格式化后的值，就直接返回
    if (!record || (record.typeValue && record.unitValue)) return record

    const [typeData, unitData] = await Promise.all([
        getCommonMetaById(record.typeId),
        getCommonMetaById(record.unitId)
    ])

    return {
        ...record,
        typeValue: record.typeValue || typeData?.value1 || '-',
        unitValue: record.unitValue || unitData?.value1 || '-'
    }
}

/**
 * 格式化资产记录中的所有元数据
 * @param {Object} record 资产记录数据
 * @returns {Promise<Object>} 格式化后的记录
 */
export async function formatAssetRecord(record) {
    if (!record) {
        console.debug('formatAssetRecord: 记录为空')
        return record
    }

    console.debug('formatAssetRecord: 开始格式化记录:', record)
    console.debug('formatAssetRecord: 货币单位ID:', record.unitId)

    const [typeData, unitData, locationData] = await Promise.all([
        getCommonMetaById(record.assetTypeId),
        getCommonMetaById(record.unitId),
        getCommonMetaById(record.assetLocationId)
    ])

    console.debug('formatAssetRecord: 获取到的元数据:', {
        typeData,
        unitData,
        locationData
    })

    const formattedRecord = {
        ...record,
        assetTypeValue: typeData?.value1 || '-',
        unitValue: unitData?.value1 || '-',
        locationValue: locationData?.value1 || '-'
    }

    console.debug('formatAssetRecord: 格式化后的记录:', formattedRecord)
    return formattedRecord
}
