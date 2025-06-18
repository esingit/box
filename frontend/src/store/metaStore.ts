// /store/metaStore.ts
import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import axiosInstance from '@/utils/axios'
import emitter from '@/utils/eventBus'

// 元数据请求 DTO 和返回 VO
interface CommonMetaDTO {
    typeCode: string
    typeName?: string
    key1?: string
    key2?: string
    key3?: string
    key4?: string
    value1?: string
    value2?: string
    value3?: string
    value4?: string
}

interface CommonMetaVO extends Required<Pick<CommonMetaDTO, 'typeCode'>> {
    id: number
    typeName: string
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

    // 默认预加载元数据类型，可按需修改
    const defaultTypeCodes = ref<string[]>([
        'FITNESS_TYPE',
        'UNIT',
        'ASSET_TYPE',
        'ASSET_LOCATION'
    ])

    /**
     * 请求通用元数据（按 typeCode 合并查询，自动缓存）
     */
    async function queryMeta(dtoList: CommonMetaDTO[], useCache = true): Promise<CommonMetaVO[]> {
        const needQueryList = dtoList.filter(dto => {
            if (!dto.typeCode) return false
            return !useCache || !typeMap[dto.typeCode]
        })

        // 所有请求都已缓存，直接返回
        if (needQueryList.length === 0) {
            return dtoList.flatMap(dto => typeMap[dto.typeCode] || [])
        }

        loading.value = true
        try {
            const res = await axiosInstance.post('/api/common-meta/query', needQueryList)
            const rawList: CommonMetaVO[] = res.data?.data || []

            if (res.data?.success) {
                // 更新缓存：按 typeCode 分类填入
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
        const dtoList = defaultTypeCodes.value.map(code => ({ typeCode: code }))
        await queryMeta(dtoList, true)
    }

    /**
     * 获取某类元数据选项列表
     */
    function getOptions(typeCode: string): CommonMetaVO[] {
        return typeMap[typeCode] || []
    }

    /**
     * 手动设置某类元数据列表（用于前端动态修改）
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
        // 状态
        loading,
        typeMap,

        // 方法
        initAll,
        queryMeta,
        getOptions,
        setTypeMap,
        clearCache
    }
})
