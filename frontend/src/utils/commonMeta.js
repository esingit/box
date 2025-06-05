import axios from './axios'

// 缓存已获取的数据
const cache = new Map()

/**
 * 根据ID获取CommonMeta的值
 * @param {string|number} id CommonMeta的ID
 * @returns {Promise<{typeName: string, value1: string}|null>} 返回对应的CommonMeta数据
 */
export async function getCommonMetaById(id) {
    if (!id) return null
    
    // 检查缓存
    if (cache.has(id)) {
        return cache.get(id)
    }

    try {
        const res = await axios.get(`/api/common-meta/by-id/${id}`)
        if (res.data?.success) {
            cache.set(id, res.data.data)
            return res.data.data
        }
        return null
    } catch (error) {
        console.error('获取CommonMeta数据失败:', error)
        return null
    }
}

/**
 * 清除缓存数据
 */
export function clearCommonMetaCache() {
    cache.clear()
}

/**
 * 格式化健身记录中的类型和单位
 * @param {Object} record 健身记录数据
 * @returns {Promise<Object>} 格式化后的记录
 */
export async function formatFitnessRecord(record) {
    if (!record) return record

    const [typeData, unitData] = await Promise.all([
        getCommonMetaById(record.typeId),
        getCommonMetaById(record.unitId)
    ])

    return {
        ...record,
        typeText: typeData?.value1 || '-',
        unitText: unitData?.value1 || '-'
    }
}
