// /store/metaStore.ts
import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import axiosInstance from '@/api/axios'
import emitter from '@/utils/eventBus'

// 请求参数 DTO，只包含 typeCode
export interface CommonMetaQueryDTO {
    typeCode: string
}

// 元数据返回 VO 类型
export interface CommonMetaVO extends Required<Pick<CommonMetaQueryDTO, 'typeCode'>> {
    id: number
    typeCode: string
    typeName: string
    value: string | number
    label: string
    key1?: string
    key2?: string
    key3?: string
    key4?: string
    value1?: string
    value2?: string
    value3?: string
    value4?: string
}

interface ApiResponse<T = any> {
    success: boolean
    message?: string
    data?: T
    code?: string
}

export const useMetaStore = defineStore('meta', () => {
    // 状态管理
    const loadingState = reactive({
        query: false,
        init: false
    })

    // 是否开发环境
    const isDev = import.meta.env.DEV

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
     * 处理API响应
     * @param response API响应对象
     * @param operationName 操作名称
     * @returns 处理后的数据或null
     */
    function handleApiResponse<T>(response: any, operationName: string): T | null {
        // 检查是否需要重新登录
        if (response?.data?.code === 'AUTH_REQUIRED') {
            if (isDev) {
                console.info(`🔐 [${operationName}] 检测到需要重新登录，已静默处理`)
            }
            return null
        }

        if (response?.data?.success) {
            return response.data.data
        }

        // 业务逻辑错误
        const errorMessage = response?.data?.message || `${operationName}失败`
        emitter.emit('notify', {
            message: errorMessage,
            type: 'error'
        })

        return null
    }

    /**
     * 处理错误
     * @param operationName 操作名称
     * @param error 错误对象
     */
    function handleError(operationName: string, error: unknown): void {
        // 忽略取消相关的错误
        if (isRequestCancelled(error)) {
            if (isDev) {
                console.log(`🟡 [${operationName}] 请求被取消`)
            }
            return
        }

        // 忽略认证相关错误，这些会由全局处理
        if (isAuthError(error)) {
            if (isDev) {
                console.log(`🟡 [${operationName}] 认证错误，等待用户登录`)
            }
            return
        }

        // 记录并显示其他错误
        const errorMessage = getErrorMessage(error)
        if (isDev) {
            console.error(`🔴 [${operationName}] 出错:`, error)
        }

        emitter.emit('notify', {
            message: `${operationName}失败：${errorMessage}`,
            type: 'error'
        })
    }

    /**
     * 判断是否是请求取消错误
     */
    function isRequestCancelled(error: unknown): boolean {
        const err = error as any
        return err?.code === 'ERR_CANCELED' ||
            err?.name === 'AbortError' ||
            err?.message?.includes('canceled')
    }

    /**
     * 判断是否是认证错误
     */
    function isAuthError(error: unknown): boolean {
        const err = error as any
        const authErrorMessages = [
            'AUTH_CANCELED',
            '用户未登录，请先登录',
            '请求已取消',
            '登录已过期，请重新登录'
        ]
        return authErrorMessages.includes(err?.message) || err?.response?.status === 401
    }

    /**
     * 获取错误消息
     */
    function getErrorMessage(error: unknown): string {
        if (error instanceof Error) {
            return error.message
        }
        return typeof error === 'string' ? error : '未知错误'
    }

    /**
     * 请求通用元数据（按 typeCode 合并查询，自动缓存）
     * @param dtoList 请求参数列表，只包含 typeCode
     * @param useCache 是否使用缓存，默认 true
     */
    async function queryMeta(dtoList: CommonMetaQueryDTO[], useCache = true): Promise<CommonMetaVO[]> {
        // 验证参数
        if (!Array.isArray(dtoList) || dtoList.length === 0) {
            return []
        }

        const needQueryList = dtoList.filter(dto => {
            if (!dto.typeCode) return false
            return !useCache || !typeMap[dto.typeCode]
        })

        // 如果全部已缓存，直接返回缓存数据
        if (needQueryList.length === 0) {
            return dtoList.flatMap(dto => typeMap[dto.typeCode] || [])
        }

        loadingState.query = true

        try {
            const controller = new AbortController()
            const response = await axiosInstance.post('/api/common-meta/query', needQueryList, {
                signal: controller.signal
            })

            const data = handleApiResponse<CommonMetaVO[]>(response, '获取元数据')
            if (!data) return []

            // 按 typeCode 分类缓存
            for (const dto of needQueryList) {
                const code = dto.typeCode
                typeMap[code] = data.filter(item => item.typeCode === code)
            }

            if (isDev) {
                console.log(`✅ 成功获取 ${needQueryList.map(d => d.typeCode).join(', ')} 元数据`)
            }

            return dtoList.flatMap(dto => typeMap[dto.typeCode] || [])
        } catch (error) {
            handleError('获取元数据', error)
            return []
        } finally {
            loadingState.query = false
        }
    }

    /**
     * 初始化默认类型的元数据
     * 通常在应用启动时调用
     */
    async function initAll(): Promise<boolean> {
        if (loadingState.init) return false

        loadingState.init = true
        try {
            if (isDev) {
                console.log('🔄 初始化元数据...')
            }

            const dtoList: CommonMetaQueryDTO[] = defaultTypeCodes.value.map(code => ({ typeCode: code }))
            const result = await queryMeta(dtoList, true)

            return result.length > 0
        } catch (error) {
            handleError('初始化元数据', error)
            return false
        } finally {
            loadingState.init = false
        }
    }

    /**
     * 获取某类元数据选项列表
     * @param typeCode 类型编码
     */
    function getOptions(typeCode: string): CommonMetaVO[] {
        if (!typeCode) return []
        return typeMap[typeCode] || []
    }

    /**
     * 手动设置某类元数据列表（用于前端动态修改）
     * @param typeCode 类型编码
     * @param list 元数据列表
     */
    function setTypeMap(typeCode: string, list: CommonMetaVO[]): void {
        if (!typeCode) return
        typeMap[typeCode] = Array.isArray(list) ? list : []
    }

    /**
     * 清空所有缓存数据
     */
    function clearCache(): void {
        Object.keys(typeMap).forEach(code => delete typeMap[code])
        if (isDev) {
            console.log('🧹 已清空元数据缓存')
        }
    }

    /**
     * 获取元数据加载状态
     */
    const isLoading = computed(() => loadingState.query || loadingState.init)

    /**
     * 获取已加载的元数据类型
     */
    const loadedTypes = computed(() => Object.keys(typeMap))

    return {
        loadingState,
        isLoading,
        typeMap,
        loadedTypes,
        initAll,
        queryMeta,
        getOptions,
        setTypeMap,
        clearCache
    }
})