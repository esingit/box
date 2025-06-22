// /store/metaStore.ts
import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import axiosInstance from '@/utils/axios'
import emitter from '@/utils/eventBus'

// 请求参数 DTO，只包含 typeCode
export interface CommonMetaQueryDTO {
    typeCode: string
}

// 元数据返回 VO 类型
export interface CommonMetaVO extends Required<Pick<CommonMetaQueryDTO, 'typeCode'>> {
    id: number
    typeName: string
    value?: string
    label?: string
    key1?: string
    key2?: string
    key3?: string
    key4?: string
    value1?: string
    value2?: string
    value3?: string
    value4?: string
}

export const useMetaStore = defineStore('meta', () => {
    const loading = ref(false)

    // 缓存每类 typeCode 的元数据列表
    const typeMap = reactive<Record<string, CommonMetaVO[]>>({})

    // 默认预加载元数据类型
    const defaultTypeCodes = ref<string[]>([
        'FITNESS_TYPE',
        'UNIT',
        'ASSET_TYPE',
        'ASSET_LOCATION'
    ])

    /**
     * 请求通用元数据（按 typeCode 合并查询，自动缓存）
     * @param dtoList 请求参数列表，只包含 typeCode
     * @param useCache 是否使用缓存，默认 true
     */
    async function queryMeta(dtoList: CommonMetaQueryDTO[], useCache = true): Promise<CommonMetaVO[]> {
        const needQueryList = dtoList.filter(dto => {
            if (!dto.typeCode) return false
            return !useCache || !typeMap[dto.typeCode]
        })

        // 如果全部已缓存，直接返回缓存数据
        if (needQueryList.length === 0) {
            return dtoList.flatMap(dto => typeMap[dto.typeCode] || [])
        }

        loading.value = true
        try {
            const res = await axiosInstance.post('/api/common-meta/query', needQueryList)
            const rawList: CommonMetaVO[] = res.data?.data || []

            if (res.data?.success) {
                // 按 typeCode 分类缓存
                for (const dto of needQueryList) {
                    const code = dto.typeCode
                    typeMap[code] = rawList.filter(item => item.typeCode === code)
                }
                return dtoList.flatMap(dto => typeMap[dto.typeCode] || [])
            } else {
                emitter.emit('notify', { message: res.data?.message || '元数据获取失败', type: 'error' })
                return []
            }
        } catch (err: any) {
            console.error('元数据请求失败:', err)
            emitter.emit('notify', {
                message: '请求失败：' + (err?.message || '未知错误'),
                type: 'error'
            })
            return []
        } finally {
            loading.value = false
        }
    }

    /**
     * 初始化默认类型的元数据
     * 通常在应用启动时调用
     */
    async function initAll() {
        const dtoList: CommonMetaQueryDTO[] = defaultTypeCodes.value.map(code => ({ typeCode: code }))
        await queryMeta(dtoList, true)
    }

    /**
     * 获取某类元数据选项列表
     * @param typeCode 类型编码
     */
    function getOptions(typeCode: string): CommonMetaVO[] {
        return typeMap[typeCode] || []
    }

    /**
     * 手动设置某类元数据列表（用于前端动态修改）
     * @param typeCode 类型编码
     * @param list 元数据列表
     */
    function setTypeMap(typeCode: string, list: CommonMetaVO[]) {
        typeMap[typeCode] = list
    }

    /**
     * 清空所有缓存数据
     */
    function clearCache() {
        Object.keys(typeMap).forEach(code => delete typeMap[code])
    }

    return {
        loading,
        typeMap,
        initAll,
        queryMeta,
        getOptions,
        setTypeMap,
        clearCache
    }
})
